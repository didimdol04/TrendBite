package com.trendbite.menuprofit.trend.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /**
     * FastAPI 응답은 snake_case(risk_factors)로 옴 — @JsonAlias 로 역직렬화 호환.
     * Spring → 프론트로 나갈 때는 camelCase(riskFactors)로 직렬화 (프론트 기대 형식).
     */
    @JsonAlias("risk_factors")
    private List<String> riskFactors;

    /** 마찬가지: FastAPI snake_case 입력, 프론트 camelCase 출력. */
    @JsonAlias("llm_report")
    private String llmReport;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScoreBreakdown {
        private Integer trend;
        private Integer profitability;
        private Integer sustainability;
        private Integer total;
    }
}