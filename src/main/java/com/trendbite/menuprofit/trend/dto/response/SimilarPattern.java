package com.trendbite.menuprofit.trend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimilarPattern {

    private String name;

    @JsonProperty("dtw_similarity")
    private Double dtwSimilarity;

    @JsonProperty("dtw_dist")
    private Double dtwDist;

    private String lifecycle;

    /** trend.py 응답에는 없을 수 있음 (main.py 스키마는 Optional) */
    @JsonProperty("peak_month")
    private String peakMonth;
}