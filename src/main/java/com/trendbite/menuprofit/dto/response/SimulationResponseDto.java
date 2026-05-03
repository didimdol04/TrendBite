package com.trendbite.menuprofit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SimulationResponseDto {
    private Boolean success;
    private DataDto data;

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataDto {
        private BaselineDto baseline;       // baselineProfit 없으면 null
        private SimulationDto simulation;
        private IncrementalDto incremental; // baselineProfit 없으면 null
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BaselineDto {
        private Long currentOperatingProfit;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SimulationDto {
        private Long adjustedRevenue;
        private Long totalOperatingProfit;
        private Double profitMargin;
        private Integer breakEvenQuantity;
        private String warningLevel;        // GREEN / YELLOW / ORANGE / RED
        private Double operationBurdenIndex;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IncrementalDto {
        private Long additionalOperatingProfit;
        private Double profitGrowthRate;
    }
}