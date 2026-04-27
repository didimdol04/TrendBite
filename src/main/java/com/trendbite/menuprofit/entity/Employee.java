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

    @Column(length = 50)
    private String name;           // 직원 이름/구분

    @Column(nullable = false, length = 10)
    private String mode;           // "simple" or "detail"

    // simple 모드
    private Integer monthlyTotal;  // 월 인건비 총액 (원)

    // detail 모드
    private Integer hourlyWage;           // 시급
    private Double weeklyHours;           // 주 소정근로시간

    @Column(nullable = false)
    private boolean hasNightShift = false; // 야간 근무 여부 (null 불가, 기본값 false)

    private Double nightShiftHoursPerWeek; // 주당 야간 근무시간 (hasNightShift=true일 때만 유효)
}
