package com.trendbite.menuprofit.dto.request.sub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class EmployeeDto {
    private String name;              // 직원 구분명
    private String mode;              // "simple" or "detail"

    // simple 모드
    private Integer monthlyTotal;     // 월 인건비 총액 (원)

    // detail 모드
    private Integer hourlyWage;               // 시급 (원)
    private Double weeklyHours;               // 주 소정근로시간
    private Boolean hasNightShift;            // 야간 근무 여부
    private Double nightShiftHoursPerWeek;    // 주당 야간 근무시간
}