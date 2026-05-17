package com.trendbite.menuprofit.trend.service;

import com.trendbite.menuprofit.dto.request.SimulationRequestDto;
import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.dto.response.SimulationResponseDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.service.LocationAnalysisService;
import com.trendbite.menuprofit.service.FinanceService;
import com.trendbite.menuprofit.service.SimulationService;
import com.trendbite.menuprofit.trend.client.FastApiReportClient;
import com.trendbite.menuprofit.trend.dto.request.AnalysisRequest;
import com.trendbite.menuprofit.trend.dto.request.ReportRequest;
import com.trendbite.menuprofit.trend.dto.request.TrendRequest;
import com.trendbite.menuprofit.trend.dto.response.AnalysisResponse;
import com.trendbite.menuprofit.trend.dto.response.ReportResponse;
import com.trendbite.menuprofit.trend.dto.response.TrendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 통합 분석 오케스트레이션. POST /api/v1/analysis/run 핵심 로직.
 *
 * 순서: Location → Finance → Trend ─┬─→ Simulation
 *                                   └─→ Report
 *
 * Simulation 과 Report 는 Trend 이후 독립적인 형제 단계.
 * Report 의 LangGraph 입력엔 Simulation 결과가 전달되지 않는다 — 두 단계는 분리되어 있고,
 * UI 측에서 simulation 의 warningLevel(신호등) 과 LLM 텍스트 리포트를 각각 노출하는 구조.
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

    public AnalysisResponse run(AnalysisRequest request) {
        // 1. 입지
        LocationAnalysisResponse location = locationAnalysisService.analyze(request.getLocation());
        Double multiplier = location.getData().getFinalResult().getLocationMultiplier();

        // 2. 재무
        FinanceResponseDto finance = financeService.calculate(request.getFinance());

        // 3. 트렌드 (캐시 hit/miss 여부는 TrendService가 결정)
        TrendResponse trend = trendService.analyze(TrendRequest.builder()
                .keyword(request.getKeyword())
                .category(request.getCategory())
                .build());

        // 4. 시뮬레이션 — Report 와 독립. warningLevel/operationBurdenIndex 산출용.
        SimulationResponseDto simulation = simulationService.simulate(
                buildSimulationRequest(request, trend, finance, multiplier));

        // 5. 리포트 (LangGraph LLM 호출)
        ReportRequest reportRequest = buildReportRequest(request.getKeyword(), trend, finance, multiplier);
        ReportResponse report = fastApiReportClient.generate(reportRequest);

        return AnalysisResponse.builder()
                .success(true)
                .data(AnalysisResponse.DataDto.builder()
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
        ReportRequest.FinanceSummary summary = ReportRequest.FinanceSummary.builder()
                .operatingProfit(monthly.getOperatingProfit())
                .bepQty(breakEven.getBreakEvenQuantityPerDay())
                .profitMargin(monthly.getProfitMargin())
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