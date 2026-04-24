package com.trendbite.menuprofit.location.service;

import com.trendbite.menuprofit.location.client.SeoulApiClient;
import com.trendbite.menuprofit.location.client.SogongongApiClient;
import com.trendbite.menuprofit.location.dto.AreaMapping;
import com.trendbite.menuprofit.location.dto.LocationRawData;
import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.DataDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.FinalResultDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.LocationInfoDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.ProcessedScoresDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.RawMetricsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 입지 분석 메인 오케스트레이션 서비스.
 *
 * 흐름:
 *   (1) PipMappingService   → 상권 코드 + 행정동 코드
 *   (2) SeoulApiClient x3   → 유동인구 + 추정매출 + 집객시설 수
 *   (3) SogongongApiClient  → 경쟁 점포 수
 *   (4) LocationCalculator  → locationMultiplier + grade
 *   (5) Response DTO 조립
 *
 * Fallback 정책:
 *  - 서울 외 좌표 / PIP 매핑 실패 → multiplier=1.0, grade=5, success=true, message 포함
 *  - API 호출 실패 개별 건은 각 Client가 기본값(0) 반환 → 계산은 계속 진행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationAnalysisService {

    private static final double FALLBACK_MULTIPLIER = 1.0;
    private static final int    FALLBACK_GRADE      = 5;

    private final PipMappingService         pipMappingService;
    private final SeoulApiClient            seoulApiClient;
    private final SogongongApiClient        sogongongApiClient;
    private final LocationCalculatorService calculator;

    public LocationAnalysisResponse analyze(LocationAnalysisRequest request) {
        double lat = request.getLat();
        double lng = request.getLng();
        String address = request.getAddress();

        // (1) PIP 매핑
        Optional<AreaMapping> mappingOpt = pipMappingService.findAreaByCoord(lat, lng);
        if (mappingOpt.isEmpty()) {
            log.info("[LocationAnalysis] 서울 외 좌표 / PIP 실패 — Fallback 반환 (lat={}, lng={})", lat, lng);
            return buildFallbackResponse(request,
                    "입력 주소가 서울시 상권/행정동 경계 밖입니다. 기본값(보정계수 1.0)이 적용됩니다.");
        }
        AreaMapping mapping = mappingOpt.get();

        // (2), (3) 외부 API 호출 (각각 실패 시 0 반환)
        long   foottraffic     = seoulApiClient.fetchFootTraffic(mapping.getAreaCode());
        long   avgMonthlySales = seoulApiClient.fetchAvgMonthlySales(mapping.getDongCode());
        int    facilityCount   = seoulApiClient.fetchFacilityCount(mapping.getAreaCode());
        int    competitionCnt  = sogongongApiClient.fetchCompetitionCount(mapping.getDongCode());

        LocationRawData raw = LocationRawData.builder()
                .foottraffic(foottraffic)
                .avgMonthlySales(avgMonthlySales)
                .facilityCount(facilityCount)
                .competitionCount(competitionCnt)
                .build();

        // (4) 계수 산출
        double trafficScore     = calculator.normalize(foottraffic,
                LocationCalculatorService.TRAFFIC_MIN, LocationCalculatorService.TRAFFIC_MAX);
        double salesScore       = calculator.normalize(avgMonthlySales,
                LocationCalculatorService.SALES_MIN,   LocationCalculatorService.SALES_MAX);
        double competitionScore = calculator.normalize(competitionCnt,
                LocationCalculatorService.COMP_MIN,    LocationCalculatorService.COMP_MAX);
        double rawScore   = calculator.calculateRawScore(trafficScore, salesScore, competitionScore);
        double multiplier = calculator.calculate(foottraffic, avgMonthlySales, competitionCnt);
        int    grade      = calculator.calculateGrade(multiplier);

        // (5) Response 조립
        return LocationAnalysisResponse.builder()
                .success(true)
                .data(DataDto.builder()
                        .locationInfo(LocationInfoDto.builder()
                                .address(address).lat(lat).lng(lng)
                                .areaCode(mapping.getAreaCode()).areaName(mapping.getAreaName())
                                .dongCode(mapping.getDongCode()).dongName(mapping.getDongName())
                                .build())
                        .rawMetrics(RawMetricsDto.builder()
                                .foottraffic(raw.getFoottraffic())
                                .avgMonthlySales(raw.getAvgMonthlySales())
                                .facilityCount(raw.getFacilityCount())
                                .competitionCount(raw.getCompetitionCount())
                                .build())
                        .processedScores(ProcessedScoresDto.builder()
                                .trafficScore(round3(trafficScore))
                                .salesScore(round3(salesScore))
                                .competitionScore(round3(competitionScore))
                                .rawScore(rawScore)
                                .build())
                        .finalResult(FinalResultDto.builder()
                                .locationMultiplier(multiplier)
                                .grade(grade)
                                .build())
                        .message(null)
                        .build())
                .build();
    }

    /** 서울 외 / 매핑 실패 / 심각한 예외 시 사용할 fallback 응답 */
    public LocationAnalysisResponse buildFallbackResponse(LocationAnalysisRequest request, String message) {
        return LocationAnalysisResponse.builder()
                .success(true)
                .data(DataDto.builder()
                        .locationInfo(LocationInfoDto.builder()
                                .address(request.getAddress())
                                .lat(request.getLat())
                                .lng(request.getLng())
                                .build())
                        .rawMetrics(RawMetricsDto.builder()
                                .foottraffic(0L).avgMonthlySales(0L)
                                .facilityCount(0).competitionCount(0)
                                .build())
                        .processedScores(ProcessedScoresDto.builder()
                                .trafficScore(0.0).salesScore(0.0)
                                .competitionScore(0.0).rawScore(0.0)
                                .build())
                        .finalResult(FinalResultDto.builder()
                                .locationMultiplier(FALLBACK_MULTIPLIER)
                                .grade(FALLBACK_GRADE)
                                .build())
                        .message(message)
                        .build())
                .build();
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
