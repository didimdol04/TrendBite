package com.trendbite.menuprofit.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class SalesRecordRequestDto {

    @NotNull
    private Long menuId;

    @NotNull
    private LocalDate salesDate;

    @NotNull
    @Min(0)
    private Integer actualQuantity; // 실제 판매량 (개)

    @NotNull
    @Min(0)
    private Integer actualRevenue;  // 실제 매출 (원)
}
