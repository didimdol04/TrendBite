package com.trendbite.menuprofit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "simulation_result")
@Getter @Setter
@NoArgsConstructor
public class SimulationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;                    // 어떤 메뉴의 결과인지

    private Double activityLaborCost;     // 활동인건비 (원)
    private Double indirectCostAlloc;     // 간접비 배분액 (원)
    private Double operatingProfit;       // 영업이익 (OP)
    private Double profitMargin;          // 이익률 (%)
    private Double breakEvenQuantity;     // 손익분기 판매량 (개)
    private Double locationScore;         // 입지계수 (0.8~1.2)
    private Double adjustedRevenue;       // 보정 매출
    private String warningLevel;          // 경고 레벨 (GREEN/YELLOW/ORANGE/RED)
    private Double operationBurdenIndex;  // 운영 부담 지수
    private String aiComment;             // AI 코멘트
}