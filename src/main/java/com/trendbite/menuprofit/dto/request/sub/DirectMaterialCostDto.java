package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class DirectMaterialCostDto {
    private Integer ingredientCostPerUnit; // 단위당 식재료비 (원)
}