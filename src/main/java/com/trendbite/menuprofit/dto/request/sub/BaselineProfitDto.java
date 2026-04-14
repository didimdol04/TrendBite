package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class BaselineProfitDto {
    private Long currentMonthlyRevenue;         // 현재 월 매출 (원) — Integer 오버플로 방지를 위해 Long 사용
    private Long currentMonthlyOperatingProfit; // 현재 월 영업이익 (원)
}
