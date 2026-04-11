package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class BaselineProfitDto {
    private Integer currentMonthlyRevenue;         // 현재 월 매출 (원)
    private Integer currentMonthlyOperatingProfit; // 현재 월 영업이익 (원)
}