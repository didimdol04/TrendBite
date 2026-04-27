package com.trendbite.menuprofit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu")
@Getter @Setter
@NoArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String menuName;           // 메뉴명

    @Column(length = 50)
    private String category;           // 카테고리 (볶음류, 면류 등)

    @Column(nullable = false)
    private Integer sellingPrice;      // 판매가 (원)

    @Column(nullable = false)
    private Integer ingredientCost;    // 식재료비 (원)

    @Column(nullable = false)
    private Integer expectedDailySales; // 예상 일 판매량 (개)

    // BOH/FOH 활동 소요시간 (분)
    private Double bohTimeMinutes;     // BOH 소요시간
    private Double fohTimeMinutes;     // FOH 소요시간

    // 변동간접원가 (단일 합산)
    private Integer variableOverheadCostPerUnit; // 단위당 변동제조간접원가 합산 (원)

    // 포장재비
    private Integer packagingCostPerUnit;        // 단위당 포장재비 (원)

    // 판매 가정값
    @Column(nullable = false)
    private Integer workingDaysPerWeek;          // 주당 영업일

    private Integer trendDurationWeeks;          // 트렌드 지속 기간 (주)

    // 기존 이익 (Integer 범위(약 21억) 초과 가능 → Long 사용)
    private Long baselineMonthlyRevenue;
    private Long baselineMonthlyOperatingProfit;

    // 고정비 (월)
    private Integer monthlyRent;       // 임대료
    private Integer monthlyUtility;    // 공과금
    private Integer monthlyMisc;       // 기타

    private Long totalMonthlyRevenue; // 전체 월 매출 (간접비 배분용)
}
