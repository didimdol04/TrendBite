package com.trendbite.menuprofit.trend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trendbite.menuprofit.trend.dto.response.SimilarPattern;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportRequest {

    @NotBlank
    private String keyword;

    @NotNull
    @Valid
    @JsonProperty("trend_analysis")
    private TrendAnalysis trendAnalysis;

    @JsonProperty("similar_patterns")
    private List<SimilarPattern> similarPatterns;

    @NotNull
    @Valid
    @JsonProperty("finance_summary")
    private FinanceSummary financeSummary;

    @NotNull
    @DecimalMin("0.8")
    @DecimalMax("1.2")
    @JsonProperty("location_multiplier")
    private Double locationMultiplier;

    /** FastAPI TrendAnalysis (similar_patterns 제외 core) */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrendAnalysis {

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
    }

    /** FastAPI FinanceSummary */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FinanceSummary {

        @JsonProperty("operating_profit")
        private Long operatingProfit;

        @JsonProperty("bep_qty")
        private Integer bepQty;

        @JsonProperty("profit_margin")
        private Double profitMargin;
    }
}
