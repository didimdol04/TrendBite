package com.trendbite.menuprofit.trend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportResponse {

    /** "도입 권장" / "신중 검토" / "불확실" / "보류" */
    private String verdict;

    private ScoreBreakdown scores;

    @JsonProperty("risk_factors")
    private List<String> riskFactors;

    @JsonProperty("llm_report")
    private String llmReport;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScoreBreakdown {
        private Integer trend;
        private Integer profitability;
        private Integer sustainability;
        private Integer total;
    }
}
