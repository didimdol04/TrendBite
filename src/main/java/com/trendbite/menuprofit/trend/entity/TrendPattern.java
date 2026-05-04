package com.trendbite.menuprofit.trend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "trend_pattern",
        uniqueConstraints = @UniqueConstraint(name = "uk_trend_pattern_name", columnNames = "name")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예: "탕후루", "흑당버블티", "마라탕" */
    @Column(nullable = false, length = 100)
    private String name;

    /** 라이프사이클 라벨 — "단기급락", "유행후정체", "지속성장" 등 */
    @Column(nullable = false, length = 50)
    private String lifecycle;

    /** 월별 관심도 시계열 JSON 배열 문자열 — 예: "[5,10,40,100,80,40,...]" */
    @Lob
    @Column(name = "series_json", nullable = false, columnDefinition = "TEXT")
    private String seriesJson;

    /** 시계열 길이 (보통 12개월) — 조회 시 디버깅/검증용 */
    @Column(nullable = false)
    private Integer seriesLength;

    /** 시드 데이터 여부 — 운영자 추가 패턴과 구분 */
    @Column(nullable = false)
    private Boolean seedData;
}
