package com.trendbite.menuprofit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesCorrectionResponseDto {
    private Boolean success;
    private DataDto data;

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataDto {
        private Long menuId;
        private Integer originalExpectedDailySales;  // 초기 예상 일 판매량
        private Integer correctedExpectedDailySales; // 보정된 예상 일 판매량
        private Double correctionRate;               // 보정률 (실판매/예측 비율)
        private Integer basedOnDays;                 // 보정 기준 누적 일수
        private LocalDate lastSalesDate;             // 마지막 실판매 기록 날짜
    }
}