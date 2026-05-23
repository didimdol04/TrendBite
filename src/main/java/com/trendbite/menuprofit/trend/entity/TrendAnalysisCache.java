package com.trendbite.menuprofit.trend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trend_analysis_cache",
        indexes = {
                @Index(name = "idx_trend_cache_keyword_category", columnList = "keyword,category"),
                @Index(name = "idx_trend_cache_expires_at", columnList = "expiresAt")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendAnalysisCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false, length = 50)
    private String category;

    // ── FastAPI TrendResponse core 필드 ─────────────────────────
    @Column(nullable = false, length = 30)
    private String plcStage;

    @Column(nullable = false, length = 30)
    private String overallTrend;

    @Column(nullable = false, length = 30)
    private String recentTrend;

    @Column(nullable = false)
    private Double latestInterest;

    @Column(nullable = false)
    private Double averageInterest;

    @Column(nullable = false, length = 30)
    private String trendSpeed;

    @Column(nullable = false)
    private Double peakRatio;

    @Column(nullable = false)
    private Double growthRate;

    @Column(nullable = false)
    private Integer trendScore;

    @Column(nullable = false, length = 100)
    private String recommendation;

    /** similar_patterns(list[dict]) 직렬화 JSON 문자열 */
    @Lob
    @Column(name = "similar_patterns_json", columnDefinition = "TEXT")
    private String similarPatternsJson;

    /** 14개월 관심도 시계열 직렬화 JSON 배열 — 예: "[10.6,13.8,...]" */
    @Lob
    @Column(name = "interest_series_json", columnDefinition = "TEXT")
    private String interestSeriesJson;

    // ── 캐시 메타 ──────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}