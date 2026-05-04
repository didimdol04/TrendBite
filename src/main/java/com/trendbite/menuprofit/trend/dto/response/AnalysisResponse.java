package com.trendbite.menuprofit.trend.dto.response;

import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /api/v1/analysis/run 통합 응답.
 *
 * 4단계 결과를 한 번에 반환:
 *   - location: 입지 분석 (multiplier, grade)
 *   - finance:  원가/BEP/이익률
 *   - trend:    네이버 데이터랩 + DTW
 *   - report:   LangGraph LLM 리포트 + 점수
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisResponse {

    private Boolean success;
    private DataDto data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DataDto {
        private LocationAnalysisResponse location;
        private FinanceResponseDto       finance;
        private TrendResponse            trend;
        private ReportResponse           report;
    }
}
