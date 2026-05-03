package com.trendbite.menuprofit.service;

import com.trendbite.menuprofit.dto.request.SimulationRequestDto;
import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.dto.response.SimulationResponseDto;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {

    // 경고 레벨 기준 상수
    private static final long   WARNING_RED_THRESHOLD    = 0L;       // 영업이익 0 이하 → RED
    private static final long   WARNING_ORANGE_THRESHOLD = 500_000L; // 50만원 이하 → ORANGE
    private static final double WARNING_YELLOW_BURDEN    = 1.5;      // 운영 부담 지수 1.5 이상 → YELLOW

    // 운영 부담 지수 계산 상수
    private static final double BURDEN_BASE_TREND_SCORE  = 2.0;  // 트렌드 점수 역산 기준
    private static final double BURDEN_REFERENCE_WEEKS   = 12.0; // 기준 트렌드 지속 기간 (주)
    private static final int    MIN_DURATION_WEEKS        = 1;    // trendDurationWeeks 최솟값 (0 방어)

    public SimulationResponseDto simulate(SimulationRequestDto req) {
        FinanceResponseDto.DataDto finance = req.getFinanceResult();
        double locationScore = req.getLocationScore() != null ? req.getLocationScore() : 1.0;

        // 1. 입지 보정 매출
        long originalRevenue = finance.getMonthlyResult().getTotalRevenue();
        long adjustedRevenue = Math.round(originalRevenue * locationScore);

        // 2. 변동비도 locationScore 비례 보정
        //    판매량이 locationScore에 비례한다고 가정 → 변동비도 동일 비율 조정
        long originalVariableCost  = finance.getMonthlyResult().getTotalVariableCost();
        long adjustedVariableCost  = Math.round(originalVariableCost * locationScore);

        // 3. 고정비는 locationScore 무관 (고정비이므로 그대로)
        long fixedCost = finance.getMonthlyResult().getFixedCostAllocation();

        // 4. 최종 영업이익
        long totalOperatingProfit = adjustedRevenue - adjustedVariableCost - fixedCost;

        // 5. 보정 이익률
        double profitMargin = adjustedRevenue > 0
                ? Math.round((double) totalOperatingProfit / adjustedRevenue * 1000.0) / 1000.0
                : 0;

        // 6. BEP (기존 그대로 유지)
        int breakEvenQuantity = finance.getBreakEven().getBreakEvenQuantityPerDay();

        // 7. 운영 부담 지수 (trendDurationWeeks 0 방어)
        double operationBurdenIndex = calcBurdenIndex(req);

        // 8. 경고 레벨
        String warningLevel = calcWarningLevel(totalOperatingProfit, operationBurdenIndex);

        // 9. 기존 이익 기준 증분 계산 (baselineOperatingProfit 있을 때만)
        SimulationResponseDto.BaselineDto    baseline    = null;
        SimulationResponseDto.IncrementalDto incremental = null;

        if (finance.getIncremental() != null && req.getBaselineOperatingProfit() != null) {
            long baselineProfit   = req.getBaselineOperatingProfit();
            // 입지 보정 후 영업이익 기준으로 증분 계산
            long additionalProfit = totalOperatingProfit - baselineProfit;

            baseline = SimulationResponseDto.BaselineDto.builder()
                    .currentOperatingProfit(baselineProfit)
                    .build();
            incremental = SimulationResponseDto.IncrementalDto.builder()
                    .additionalOperatingProfit(additionalProfit)
                    .profitGrowthRate(baselineProfit > 0
                            ? Math.round((double) additionalProfit / baselineProfit * 1000.0) / 1000.0
                            : 0)
                    .build();
        }

        return SimulationResponseDto.builder()
                .success(true)
                .data(SimulationResponseDto.DataDto.builder()
                        .baseline(baseline)
                        .simulation(SimulationResponseDto.SimulationDto.builder()
                                .adjustedRevenue(adjustedRevenue)
                                .totalOperatingProfit(totalOperatingProfit)
                                .profitMargin(profitMargin)
                                .breakEvenQuantity(breakEvenQuantity)
                                .warningLevel(warningLevel)
                                .operationBurdenIndex(operationBurdenIndex)
                                .build())
                        .incremental(incremental)
                        .build())
                .build();
    }

    private double calcBurdenIndex(SimulationRequestDto req) {
        // trendDurationWeeks 최솟값 보장 (0 나눗셈 방어)
        int durationWeeks = req.getTrendDurationWeeks() != null
                ? Math.max(req.getTrendDurationWeeks(), MIN_DURATION_WEEKS)
                : (int) BURDEN_REFERENCE_WEEKS;
        double trendScore = req.getTrendScore() != null ? req.getTrendScore() : 0.5;

        // 트렌드 점수 낮고 지속 기간 짧을수록 부담 지수 상승
        return Math.round((BURDEN_BASE_TREND_SCORE - trendScore)
                * (BURDEN_REFERENCE_WEEKS / durationWeeks) * 100.0) / 100.0;
    }

    private String calcWarningLevel(long operatingProfit, double burdenIndex) {
        if (operatingProfit <= WARNING_RED_THRESHOLD)    return "RED";
        if (operatingProfit <= WARNING_ORANGE_THRESHOLD) return "ORANGE";
        if (burdenIndex >= WARNING_YELLOW_BURDEN)        return "YELLOW";
        return "GREEN";
    }
}