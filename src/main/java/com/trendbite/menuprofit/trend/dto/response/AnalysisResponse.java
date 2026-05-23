package com.trendbite.menuprofit.trend.dto.response;

import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.dto.response.SimulationResponseDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /api/v1/analysis/run 통합 응답.
 *
 * 5단계 결과를 한 번에 반환:
 *   - location:   입지 분석 (multiplier, grade)
 *   - finance:    원가/BEP/이익률
 *   - trend:      네이버 데이터랩 + DTW
 *   - simulation: 입지·트렌드 반영 최종 시뮬레이션 (warningLevel 신호등 UI 용)
 *   - report:     LangGraph LLM 리포트 + 점수
 *
 * simulation 과 report 는 trend 이후 독립적인 형제 단계.
 * Report 는 simulation 결과를 입력으로 받지 않는다 (LangGraph 프롬프트 변경 없음).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisResponse {

    private Boolean success;
    private DataDto data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DataDto {
        /**
         * 분석 시 저장된 Menu 엔티티 ID — 후속 /sales/record 호출 시 사용.
         * Menu 저장 실패 시 null (분석 자체는 성공 응답으로 진행).
         */
        private Long                     menuId;
        private LocationAnalysisResponse location;
        private FinanceResponseDto       finance;
        private TrendResponse            trend;
        private SimulationResponseDto    simulation;
        private ReportResponse           report;
    }
}