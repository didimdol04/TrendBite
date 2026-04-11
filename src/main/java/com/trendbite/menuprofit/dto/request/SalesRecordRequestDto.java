package com.trendbite.menuprofit.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class SalesRecordRequestDto {
    private Long menuId;
    private LocalDate salesDate;
    private Integer actualQuantity; // 실제 판매량 (개)
    private Integer actualRevenue;  // 실제 매출 (원)
}