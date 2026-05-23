package com.trendbite.menuprofit.trend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrendResponse {

    @JsonProperty("plc_stage")
    private String plcStage;

    @JsonProperty("overall_trend")
    private String overallTrend;

    @JsonProperty("recent_trend")
    private String recentTrend;

    @JsonProperty("latest_interest")
    private Double latestInterest;

    @JsonProperty("average_interest")
    private Double averageInterest;

    @JsonProperty("trend_speed")
    private String trendSpeed;

    @JsonProperty("peak_ratio")
    private Double peakRatio;

    @JsonProperty("growth_rate")
    private Double growthRate;

    @JsonProperty("trend_score")
    private Integer trendScore;

    private String recommendation;

    @JsonProperty("similar_patterns")
    private List<SimilarPattern> similarPatterns;

    /** 14개월 raw 관심도 시계열. FastAPI 신규 필드 — 옛 응답에서는 null. */
    @JsonProperty("interest_series")
    private List<Double> interestSeries;
}