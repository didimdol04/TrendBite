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

    // ── TODO: 추후 추가 예정 ──────────────────────────────
    // private Integer depreciationTotal;    // 인테리어+설비 총액
    // private Integer depreciationMonths;   // 상각 기간 (월)
    // private Boolean deliveryEnabled;      // 배달 여부
    // private Double deliveryFeeRate;       // 배달 수수료율
    // private Double deliveryRevenueRatio;  // 배달 매출 비중
    // ────────────────────────────────────────────────────
}