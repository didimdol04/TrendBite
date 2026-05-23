package com.trendbite.menuprofit.trend.service;

import com.trendbite.menuprofit.dto.request.SimulationRequestDto;
import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.dto.response.SimulationResponseDto;
import com.trendbite.menuprofit.entity.Menu;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.service.LocationAnalysisService;
import com.trendbite.menuprofit.repository.MenuRepository;
import com.trendbite.menuprofit.service.FinanceService;
import com.trendbite.menuprofit.service.SimulationService;
import com.trendbite.menuprofit.trend.client.FastApiReportClient;
import com.trendbite.menuprofit.trend.dto.request.AnalysisRequest;
import com.trendbite.menuprofit.trend.dto.request.ReportRequest;
import com.trendbite.menuprofit.trend.dto.request.TrendRequest;
import com.trendbite.menuprofit.trend.dto.response.AnalysisResponse;
import com.trendbite.menuprofit.trend.dto.response.ReportResponse;
import com.trendbite.menuprofit.trend.dto.response.TrendResponse;
import com.trendbite.menuprofit.trend.exception.FastApiCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 통합 분석 오케스트레이션. POST /api/v1/analysis/run 핵심 로직.
 *
 * 순서: Location → Finance → Menu 저장 → Trend ─┬─→ Simulation
 *                                                └─→ Report
 *
 * Simulation 과 Report 는 Trend 이후 독립적인 형제 단계.
 * Report 의 LangGraph 입력엔 Simulation 결과가 전달되지 않는다 — 두 단계는 분리되어 있고,
 * UI 측에서 simulation 의 warningLevel(신호등) 과 LLM 텍스트 리포트를 각각 노출하는 구조.
 *
 * 외부 실패 내성:
 *   - Menu 저장 실패     → menuId=null 로 진행 (사장이 추후 실판매 기록 못 함)
 *   - FastAPI trend 실패  → 빈 TrendResponse 폴백 → 부분 분석 모드
 *   - FastAPI report 실패 → "불확실" verdict 플레이스홀더 → 입지·재무 결과는 보여줌
 *
 * 각 단계는 독립 서비스에 위임하고, 이 서비스는 단지 입출력을 연결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrationService {

    /** trend_score 가 null 일 때 SimulationService 가 적용하는 기본값과 동일. */
    private static final double DEFAULT_TREND_SCORE_NORMALIZED = 0.5;

    private final LocationAnalysisService locationAnalysisService;
    private final FinanceService          financeService;
    private final SimulationService       simulationService;
    private final TrendService            trendService;
    private final FastApiReportClient     fastApiReportClient;
    private final MenuRepository          menuRepository;

    public AnalysisResponse run(AnalysisRequest request) {
        // 1. 입지
        LocationAnalysisResponse location = locationAnalysisService.analyze(request.getLocation());
        Double multiplier = location.getData().getFinalResult().getLocationMultiplier();

        // 2. 재무
        FinanceResponseDto finance = financeService.calculate(request.getFinance());

        // 3. Menu 엔티티 저장 — 실판매 기록(SalesService) 연동을 위한 DB ID 발급
        Long menuId = saveMenuEntity(request);

        // 4. 트렌드 — FastAPI 실패 시 빈 응답 폴백 (분석 전체 중단 방지)
        TrendResponse trend;
        try {
            trend = trendService.analyze(TrendRequest.builder()
                    .keyword(request.getKeyword())
                    .category(request.getCategory())
                    .build());
        } catch (FastApiCallException e) {
            log.warn("[AnalysisOrchestration] FastAPI trend 실패 — 부분 분석 모드: {}", e.getMessage());
            trend = TrendResponse.builder()
                    .similarPatterns(Collections.emptyList())
                    .build();
        }

        // 5. 시뮬레이션 — trend_score null 이면 0.5 fallback
        SimulationResponseDto simulation = simulationService.simulate(
                buildSimulationRequest(request, trend, finance, multiplier));

        // 6. 리포트 — FastAPI 실패 시 플레이스홀더 (사장이 입지·재무만으로라도 판단 가능)
        ReportResponse report;
        try {
            report = fastApiReportClient.generate(
                    buildReportRequest(request.getKeyword(), trend, finance, multiplier));
        } catch (FastApiCallException e) {
            log.warn("[AnalysisOrchestration] FastAPI report 실패 — 플레이스홀더 사용: {}", e.getMessage());
            report = ReportResponse.builder()
                    .verdict("불확실")
                    .scores(ReportResponse.ScoreBreakdown.builder()
                            .trend(0).profitability(0).sustainability(0).total(0).build())
                    .riskFactors(List.of("AI 리포트 서비스에 연결할 수 없습니다."))
                    .llmReport("AI 리포트 서비스 연결에 실패했습니다. 입지·재무 수치를 바탕으로 검토해주세요.")
                    .build();
        }

        return AnalysisResponse.builder()
                .success(true)
                .data(AnalysisResponse.DataDto.builder()
                        .menuId(menuId)
                        .location(location)
                        .finance(finance)
                        .trend(trend)
                        .simulation(simulation)
                        .report(report)
                        .build())
                .build();
    }

    // ── private ────────────────────────────────────────────────

    /**
     * 분석 요청의 메뉴 정보를 협업자 Menu 엔티티로 매핑·저장.
     * 실패 시 null 반환 — 분석 흐름을 멈추지 않는다 (실판매 기록 기능만 비활성).
     *
     * 매핑:
     *   - menuName/category/sellingPrice/packagingCostPerUnit ← MenuInfoDto
     *   - ingredientCost ← directMaterialCost.ingredientCostPerUnit
     *   - bohTimeMinutes/fohTimeMinutes ← directLaborCost
     *   - variableOverheadCostPerUnit ← finance root
     *   - monthlyRent/monthlyUtility/monthlyMisc ← fixedCost (marketingCost 는 Menu 에 없어 drop)
     *   - expectedDailySales/workingDaysPerWeek/trendDurationWeeks ← salesAssumption
     *   - baselineMonthlyRevenue/baselineMonthlyOperatingProfit ← baselineProfit (optional)
     */
    private Long saveMenuEntity(AnalysisRequest req) {
        try {
            Menu menu = new Menu();
            menu.setMenuName(req.getFinance().getMenuInfo().getMenuName());
            menu.setCategory(req.getFinance().getMenuInfo().getCategory());
            menu.setSellingPrice(req.getFinance().getMenuInfo().getSellingPrice());
            menu.setIngredientCost(req.getFinance().getDirectMaterialCost().getIngredientCostPerUnit());
            menu.setExpectedDailySales(req.getFinance().getSalesAssumption().getExpectedDailySales());
            menu.setWorkingDaysPerWeek(req.getFinance().getSalesAssumption().getWorkingDaysPerWeek());
            menu.setTrendDurationWeeks(req.getFinance().getSalesAssumption().getTrendDurationWeeks());
            menu.setBohTimeMinutes(req.getFinance().getDirectLaborCost().getBohTimeMinutes());
            menu.setFohTimeMinutes(req.getFinance().getDirectLaborCost().getFohTimeMinutes());
            menu.setVariableOverheadCostPerUnit(req.getFinance().getVariableOverheadCostPerUnit());
            menu.setPackagingCostPerUnit(req.getFinance().getMenuInfo().getPackagingCostPerUnit());
            menu.setMonthlyRent(req.getFinance().getFixedCost().getMonthlyRent());
            menu.setMonthlyUtility(req.getFinance().getFixedCost().getManagementFee());
            menu.setMonthlyMisc(req.getFinance().getFixedCost().getMiscCost());
            if (req.getFinance().getBaselineProfit() != null) {
                menu.setBaselineMonthlyRevenue(
                        req.getFinance().getBaselineProfit().getCurrentMonthlyRevenue());
                menu.setBaselineMonthlyOperatingProfit(
                        req.getFinance().getBaselineProfit().getCurrentMonthlyOperatingProfit());
            }
            return menuRepository.save(menu).getId();
        } catch (Exception e) {
            log.warn("[AnalysisOrchestration] Menu 저장 실패 — menuId 없이 계속: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 협업자 SimulationService 입력 어댑터.
     *
     * 단위 변환:
     *   FastAPI trend_score(0~100, Integer) → SimulationService 기대값(0.0~1.0, Double)
     *   명시적으로 /100.0 변환. SimulationService 시그니처는 건드리지 않는다.
     *
     * Null safety:
     *   trend.trend_score 가 null 이면 0.5 fallback (SimulationService 내부 기본값과 일치).
     *
     * baselineOperatingProfit:
     *   FinanceRequestDto.baselineProfit 가 있을 때만 채움. 없으면 null 전달.
     */
    private SimulationRequestDto buildSimulationRequest(
            AnalysisRequest request,
            TrendResponse trend,
            FinanceResponseDto finance,
            Double locationMultiplier
    ) {
        Integer rawTrendScore = trend.getTrendScore();
        // 0~100 → 0.0~1.0 변환
        double trendScoreNormalized = rawTrendScore == null
                ? DEFAULT_TREND_SCORE_NORMALIZED
                : rawTrendScore / 100.0;

        SimulationRequestDto simReq = new SimulationRequestDto();
        simReq.setFinanceResult(finance.getData());
        simReq.setLocationScore(locationMultiplier);
        simReq.setTrendScore(trendScoreNormalized);
        simReq.setTrendDurationWeeks(
                request.getFinance().getSalesAssumption().getTrendDurationWeeks());

        if (request.getFinance().getBaselineProfit() != null) {
            simReq.setBaselineOperatingProfit(
                    request.getFinance().getBaselineProfit().getCurrentMonthlyOperatingProfit());
        }
        return simReq;
    }

    private ReportRequest buildReportRequest(
            String keyword,
            TrendResponse trend,
            FinanceResponseDto finance,
            Double locationMultiplier
    ) {
        ReportRequest.TrendAnalysis core = ReportRequest.TrendAnalysis.builder()
                .plcStage(trend.getPlcStage())
                .overallTrend(trend.getOverallTrend())
                .recentTrend(trend.getRecentTrend())
                .latestInterest(trend.getLatestInterest())
                .averageInterest(trend.getAverageInterest())
                .trendSpeed(trend.getTrendSpeed())
                .peakRatio(trend.getPeakRatio())
                .growthRate(trend.getGrowthRate())
                .trendScore(trend.getTrendScore())
                .recommendation(trend.getRecommendation())
                .build();

        FinanceResponseDto.MonthlyResultDto monthly   = finance.getData().getMonthlyResult();
        FinanceResponseDto.BreakEvenDto     breakEven = finance.getData().getBreakEven();

        // FastAPI FinanceSummary 의 3 필드는 모두 required(int/long/float).
        // FinanceService 는 공헌이익 <= 0 인 경우 bepQty 를 null 로 반환하는데,
        // 그대로 FastAPI 에 보내면 422. 의미 보존하면서 통과시키기 위해:
        //   - bepQty=null  → 999_999 (사실상 도달 불가 = "사업성 매우 낮음" 시그널.
        //                    risk_factors 의 'bep > 400' 조건도 자동 트리거됨)
        //   - operatingProfit/profitMargin 은 현재 null 안 나오지만 방어적으로 0 fallback
        Long    operatingProfit = monthly.getOperatingProfit() != null
                ? monthly.getOperatingProfit() : 0L;
        Integer bepQty          = breakEven.getBreakEvenQuantityPerDay() != null
                ? breakEven.getBreakEvenQuantityPerDay() : 999_999;
        Double  profitMargin    = monthly.getProfitMargin() != null
                ? monthly.getProfitMargin() : 0.0;

        ReportRequest.FinanceSummary summary = ReportRequest.FinanceSummary.builder()
                .operatingProfit(operatingProfit)
                .bepQty(bepQty)
                .profitMargin(profitMargin)
                .build();

        return ReportRequest.builder()
                .keyword(keyword)
                .trendAnalysis(core)
                .similarPatterns(trend.getSimilarPatterns() != null
                        ? trend.getSimilarPatterns()
                        : Collections.emptyList())
                .financeSummary(summary)
                .locationMultiplier(locationMultiplier)
                .build();
    }
}