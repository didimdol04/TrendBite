package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class DirectLaborCostDto {
    private Double bohTimeMinutes; // BOH 소요시간 (분)
    private Double fohTimeMinutes; // FOH 소요시간 (분)
    private List<EmployeeDto> employees; // 직원별 인건비 목록
}