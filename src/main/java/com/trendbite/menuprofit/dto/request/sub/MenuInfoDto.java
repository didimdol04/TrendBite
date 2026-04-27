package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class MenuInfoDto {
    private String menuName;
    private String category;
    private Integer sellingPrice;         // 판매가 (원)
    private Integer packagingCostPerUnit; // 단위당 포장재비 (원)
}