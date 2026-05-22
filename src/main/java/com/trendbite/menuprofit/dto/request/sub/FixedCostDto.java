package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class FixedCostDto {
    private Integer monthlyRent;      // 월 임대료 (원)
    private Integer managementFee;    // 월 관리비·공과금 (원)
    private Integer marketingCost;    // 월 마케팅비 (원)
    private Integer miscCost;         // 월 기타 비용 (원)
}