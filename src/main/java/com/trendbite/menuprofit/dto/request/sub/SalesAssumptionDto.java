package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class SalesAssumptionDto {
    private Integer expectedDailySales;  // 예상 일 판매량 (개)
    private Integer workingDaysPerWeek;  // 주당 영업일
    private Integer trendDurationWeeks;  // 트렌드 지속 기간 (주)
}