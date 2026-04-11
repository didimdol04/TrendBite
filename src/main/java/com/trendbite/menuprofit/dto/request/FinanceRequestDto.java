package com.trendbite.menuprofit.dto.request;

import com.trendbite.menuprofit.dto.request.sub.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class FinanceRequestDto {
    private MenuInfoDto menuInfo;
    private DirectMaterialCostDto directMaterialCost;
    private DirectLaborCostDto directLaborCost;
    private Integer variableOverheadCostPerUnit; // 단위당 변동제조간접원가 합산
    private FixedCostDto fixedCost;
    private SalesAssumptionDto salesAssumption;
    private BaselineProfitDto baselineProfit;    // 선택 입력 (null 허용)
}