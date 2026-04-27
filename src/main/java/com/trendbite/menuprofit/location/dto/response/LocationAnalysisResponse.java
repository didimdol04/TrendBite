package com.trendbite.menuprofit.location.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/location/analyze 응답 DTO
 *
 * 팀 공통 스타일 (FinanceResponseDto / SimulationResponseDto와 동일):
 *  - success flag + 중첩 DataDto
 *  - Builder 패턴
 *
 * 핵심 필드: data.finalResult.locationMultiplier — /simulation/run의 locationScore로 전달됨.
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class LocationAnalysisResponse {

    private Boolean success;
    private DataDto data;

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataDto {
        private LocationInfoDto     locationInfo;
        private RawMetricsDto       rawMetrics;
        private ProcessedScoresDto  processedScores;
        private FinalResultDto      finalResult;
        private String              message;   // Fallback 시 사용자 안내문구
    }

    /** 주소 및 매핑된 상권/행정동 정보 */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LocationInfoDto {
        private String address;
        private Double lat;
        private Double lng;
        private String areaCode;
        private String areaName;
        private String dongCode;
        private String dongName;
    }

    /** 외부 API에서 수집한 원본 값 */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RawMetricsDto {
        private Long    foottraffic;
        private Long    avgMonthlySales;
        private Integer facilityCount;
        private Integer competitionCount;
    }

    /** Min-Max 정규화된 0~1 점수 + raw score */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProcessedScoresDto {
        private Double trafficScore;
        private Double salesScore;
        private Double competitionScore;
        private Double rawScore;
    }

    /** 최종 결과: locationMultiplier (0.8~1.2) + 등급(1~10) */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FinalResultDto {
        private Double locationMultiplier;
        private Integer grade;
    }
}
