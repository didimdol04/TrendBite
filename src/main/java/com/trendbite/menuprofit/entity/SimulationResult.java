package com.trendbite.menuprofit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    // GREEN / YELLOW / ORANGE / RED — 추후 Enum으로 전환 권장
    @Column(length = 10)
    private String warningLevel;

    private Double operationBurdenIndex;  // 운영 부담 지수

    @Column(columnDefinition = "TEXT")
    private String aiComment;             // AI 코멘트 (길이 제한 없음)

    @Column(nullable = false, updatable = false)
    private LocalDateTime simulatedAt;    // 시뮬레이션 실행 시각

    @PrePersist
    private void prePersist() {
        this.simulatedAt = LocalDateTime.now();
    }
}
