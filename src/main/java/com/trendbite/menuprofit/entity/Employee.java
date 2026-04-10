package com.trendbite.menuprofit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee")
@Getter @Setter
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    private String name;           // 직원 이름/구분
    private String mode;           // "simple" or "detail"

    // simple 모드
    private Integer monthlyTotal;  // 월 인건비 총액 (원)

    // detail 모드
    private Integer hourlyWage;           // 시급
    private Double weeklyHours;           // 주 소정근로시간
    private Boolean hasNightShift;        // 야간 근무 여부
    private Double nightShiftHoursPerWeek; // 주당 야간 근무시간
}