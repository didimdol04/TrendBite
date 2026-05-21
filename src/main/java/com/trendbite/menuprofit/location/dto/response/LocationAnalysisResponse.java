package com.trendbite.menuprofit.location.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/v1/location/analyze 응답 DTO
 *
 * 팀 공통 스타일 (FinanceResponseDto / SimulationResponseDto와 동일):
 *  - success flag + 중첩 DataDto
 *  - Builder 패턴
 *
 * 핵심 필드: data.finalResult.locationMultiplier — /simulation/run의 locationScore로 전달됨.
 *
 * v2 변경 사항
 * 
 *   (P0-4) {@link RawMetricsDto} 안에 {@link DataSourceStatus} 추가 — 어느 외부 API가 정상 응답했는지 표시
 *   (P1-1) {@link LocationInfoDto}에 {@code nearestFallback}, {@code nearestDistanceMeters} 추가
 *   (P2-2) {@link ProcessedScoresDto}에 {@code normalizedScore}(0~1), {@code displayScore}(0~100) 추가
 * 
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

    /**
     * 주소 및 매핑된 상권/행정동 정보.
     * (P1-1) {@code nearestFallback}, {@code nearestDistanceMeters} 추가.
     */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LocationInfoDto {
        private String address;
        private Double lat;
        private Double lng;
        private String areaCode;
        private String areaName;
        private String dongCode;
        private String dongName;

        /**
         * 상권이 PIP 정타가 아닌 최근접 폴리곤 fallback으로 결정됐는지.
         * true면 프론트는 "약 OOOm 떨어진 인근 상권 데이터 기반 추정값" 안내 표시 권장.
         */
        private Boolean nearestFallback;

        /** fallback 시 가장 가까운 상권 폴리곤까지 거리(미터). PIP 정타 시 null. */
        private Double nearestDistanceMeters;
    }

    /**
     * 외부 API에서 수집한 원본 값.
     * (P0-4) 각 API의 응답 정상 여부를 추적하는 {@link DataSourceStatus} 필드 추가.
     */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RawMetricsDto {
        private Long    foottraffic;
        private Long    avgMonthlySales;
        private Integer facilityCount;
        private Integer competitionCount;

        /**
         * 각 외부 API 데이터 소스의 정상 응답 여부.
         * 현재는 휴리스틱(value &gt; 0)로 추정. P0-1~P0-3 작업 시
         * SeoulApiClient/SogongongApiClient를 명시적 ApiCallResult 패턴으로
         * 리팩터링하면 정확한 상태가 채워진다.
         */
        private DataSourceStatus dataSourceStatus;
    }

    /**
     * 외부 API 호출 결과 상태 표시.
     * 
     * 휴리스틱 정의:
     * 
     *   {@code true}  — value &gt; 0 (응답 받았음)
     *   {@code null}  — value == 0 (응답이 0인지, API 실패인지 현재 단계에서는 구분 불가)
     * 
     * P0-1~P0-3 작업 후 명시적 boolean(true/false)로 정확화 예정.
     */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DataSourceStatus {
        private Boolean trafficFromApi;       // 서울시 길단위인구
        private Boolean salesFromApi;         // 서울시 추정매출
        private Boolean facilityFromApi;      // 서울시 집객시설
        private Boolean competitionFromApi;   // 소진공 상가정보
    }

    /**
     * Min-Max 정규화된 0~1 점수 + raw score.
     * (P2-2) {@code normalizedScore} (0~1), {@code displayScore} (0~100) 추가 —
     *        rawScore가 음수(-0.2~0.8)로 사용자에게 표시되는 문제 해결.
     */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProcessedScoresDto {
        private Double trafficScore;
        private Double salesScore;
        private Double competitionScore;
        private Double rawScore;            // 이론 범위 -0.2 ~ 0.8 (디버깅용)

        /** rawScore를 0~1 범위로 선형 정규화한 값. 사용자 표시 권장. */
        private Double normalizedScore;

        /** rawScore를 0~100점으로 환산한 값 (= normalizedScore × 100, 정수 반올림). */
        private Integer displayScore;
    }

    /** 최종 결과: locationMultiplier (0.8~1.2) + 등급(1~10) */
    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FinalResultDto {
        private Double locationMultiplier;
        private Integer grade;
    }
}
