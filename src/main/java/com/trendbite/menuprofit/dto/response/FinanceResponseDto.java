package com.trendbite.menuprofit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class FinanceResponseDto {
    private Boolean success;
    private DataDto data;

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataDto {
        private UnitEconomicsDto unitEconomics;
        private MonthlyResultDto monthlyResult;
        private BreakEvenDto breakEven;
        private IncrementalDto incremental; // baselineProfit 없으면 null
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UnitEconomicsDto {
        private Integer sellingPrice;
        private Integer directMaterialCostPerUnit;
        private Integer directLaborCostPerUnit;
        private Integer variableOverheadPerUnit;
        private Integer totalVariableCostPerUnit;
        private Integer contributionMarginPerUnit;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyResultDto {
        private Long totalRevenue;
        private Long totalVariableCost;
        private Long fixedCostAllocation;
        private Long operatingProfit;
        private Double profitMargin;
        private Double foodCostRate;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BreakEvenDto {
        private Integer breakEvenQuantityPerDay;
        private Long breakEvenRevenuePerMonth;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IncrementalDto {
        private Long additionalRevenue;
        private Long additionalOperatingProfit;
        private Double profitGrowthRate;
    }
}