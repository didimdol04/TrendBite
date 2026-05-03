package com.trendbite.menuprofit.dto.request;

import com.trendbite.menuprofit.dto.request.sub.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class FinanceRequestDto {

    @NotNull
    @Valid
    private MenuInfoDto menuInfo;

    @NotNull
    @Valid
    private DirectMaterialCostDto directMaterialCost;

    @NotNull
    @Valid
    private DirectLaborCostDto directLaborCost;

    @NotNull
    private Integer variableOverheadCostPerUnit; // 단위당 변동제조간접원가 합산

    @NotNull
    @Valid
    private FixedCostDto fixedCost;

    @NotNull
    @Valid
    private SalesAssumptionDto salesAssumption;

    @Valid
    private BaselineProfitDto baselineProfit;    // 선택 입력 (null 허용)
}
