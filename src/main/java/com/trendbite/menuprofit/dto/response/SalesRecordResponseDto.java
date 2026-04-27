package com.trendbite.menuprofit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesRecordResponseDto {
    private Boolean success;
    private DataDto data;

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataDto {
        private Long id;
        private Long menuId;
        private LocalDate salesDate;
        private Integer actualQuantity;
        private Integer actualRevenue;
    }
}