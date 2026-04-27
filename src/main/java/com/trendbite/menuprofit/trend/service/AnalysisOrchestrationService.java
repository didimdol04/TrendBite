package com.trendbite.menuprofit.trend.service;

import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.service.LocationAnalysisService;
import com.trendbite.menuprofit.service.FinanceService;
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
 * 순서: Location → Finance → Trend → Report
 *
 * 각 단계는 독립 서비스에 위임하고, 이 서비스는 단지 입출력을 연결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrationService {

    private final LocationAnalysisService locationAnalysisService;
    private final FinanceService          financeService;
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

        // 4. 리포트
        ReportRequest reportRequest = buildReportRequest(request.getKeyword(), trend, finance, multiplier);
        ReportResponse report = fastApiReportClient.generate(reportRequest);

        return AnalysisResponse.builder()
                .success(true)
                .data(AnalysisResponse.DataDto.builder()
                        .location(location)
                        .finance(finance)
                        .trend(trend)
                        .report(report)
                        .build())
                .build();
    }

    // ── private ────────────────────────────────────────────────

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
