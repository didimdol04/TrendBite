package com.trendbite.menuprofit.location.service;

import com.trendbite.menuprofit.location.client.SeoulApiClient;
import com.trendbite.menuprofit.location.client.SogongongApiClient;
import com.trendbite.menuprofit.location.dto.AreaMapping;
import com.trendbite.menuprofit.location.dto.LocationRawData;
import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.DataDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.DataSourceStatus;
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
 *   (1) PipMappingService   → 상권 코드 + 행정동 코드 + fallback 메타데이터
 *   (2) SeoulApiClient x3   → 유동인구 + 추정매출 + 집객시설 수
 *   (3) SogongongApiClient  → 경쟁 점포 수
 *   (4) LocationCalculator  → locationMultiplier + grade
 *   (5) Response DTO 조립
 *
 * Fallback 정책:
 *  - 서울 외 좌표 / PIP 매핑 실패 → multiplier=1.0, grade=5, success=true, message 포함
 *  - API 호출 실패 개별 건은 각 Client가 기본값(0) 반환 → 계산은 계속 진행
 *
 * <h3>v2 변경 사항</h3>
 * <ul>
 *   <li>(P3-7) 정상 흐름 종료 직전 multiplier·grade·raw 데이터 일괄 로그</li>
 *   <li>(P4-1) {@link LocationCalculatorService#calculateFromScores(double, double, double)}로
 *              normalize 중복 호출(6→3) 제거</li>
 *   <li>(P0-4) {@link DataSourceStatus} 채워서 응답 — 휴리스틱(value &gt; 0)</li>
 *   <li>(P1-1) {@link AreaMapping}의 {@code nearestFallback}/{@code nearestDistanceMeters}
 *              {@link LocationInfoDto}로 전파</li>
 *   <li>(P2-2) {@code rawScore}→{@code normalizedScore}(0~1)·{@code displayScore}(0~100) 표시 추가</li>
 * </ul>
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

        // (4) 계수 산출 — (P4-1) normalize 1회만 호출 (정규화된 값을 calculateFromScores에 직접 전달)
        double trafficScore     = calculator.normalize(foottraffic,
                LocationCalculatorService.TRAFFIC_MIN, LocationCalculatorService.TRAFFIC_MAX);
        double salesScore       = calculator.normalize(avgMonthlySales,
                LocationCalculatorService.SALES_MIN,   LocationCalculatorService.SALES_MAX);
        double competitionScore = calculator.normalize(competitionCnt,
                LocationCalculatorService.COMP_MIN,    LocationCalculatorService.COMP_MAX);
        double rawScore   = calculator.calculateRawScore(trafficScore, salesScore, competitionScore);
        double multiplier = calculator.calculateFromScores(trafficScore, salesScore, competitionScore);
        int    grade      = calculator.calculateGrade(multiplier);

        // (P2-2) 표시용 정규화 점수
        double normalizedScore = round3((rawScore + 0.2) / 1.0);     // 0~1 (음수 제거)
        int    displayScore    = (int) Math.round(normalizedScore * 100); // 0~100

        // (P0-4) 외부 API 정상 응답 여부 — 휴리스틱(value > 0)
        DataSourceStatus dataSourceStatus = buildDataSourceStatus(
                foottraffic, avgMonthlySales, facilityCount, competitionCnt);

        // (P3-7) 정상 흐름 일괄 로그 — 디버깅·운영 모니터링용
        log.info("[LocationAnalysis] 분석 완료 — areaCode={}, dongCode={}, "
                        + "nearestFallback={}, distance={}m, "
                        + "raw(traffic={}, sales={}원, facility={}, competition={}), "
                        + "scores({}/{}/{}), rawScore={}, multiplier={}, grade={}",
                mapping.getAreaCode(), mapping.getDongCode(),
                mapping.isNearestFallback(),
                mapping.getNearestDistanceMeters() != null
                        ? String.format("%.1f", mapping.getNearestDistanceMeters()) : "n/a",
                foottraffic, avgMonthlySales, facilityCount, competitionCnt,
                round3(trafficScore), round3(salesScore), round3(competitionScore),
                rawScore, multiplier, grade);

        // (5) Response 조립 — (P1-1) fallback 메타 + (P0-4) dataSourceStatus + (P2-2) normalizedScore/displayScore
        return LocationAnalysisResponse.builder()
                .success(true)
                .data(DataDto.builder()
                        .locationInfo(LocationInfoDto.builder()
                                .address(address).lat(lat).lng(lng)
                                .areaCode(mapping.getAreaCode()).areaName(mapping.getAreaName())
                                .dongCode(mapping.getDongCode()).dongName(mapping.getDongName())
                                .nearestFallback(mapping.isNearestFallback())
                                .nearestDistanceMeters(mapping.getNearestDistanceMeters())
                                .build())
                        .rawMetrics(RawMetricsDto.builder()
                                .foottraffic(raw.getFoottraffic())
                                .avgMonthlySales(raw.getAvgMonthlySales())
                                .facilityCount(raw.getFacilityCount())
                                .competitionCount(raw.getCompetitionCount())
                                .dataSourceStatus(dataSourceStatus)
                                .build())
                        .processedScores(ProcessedScoresDto.builder()
                                .trafficScore(round3(trafficScore))
                                .salesScore(round3(salesScore))
                                .competitionScore(round3(competitionScore))
                                .rawScore(rawScore)
                                .normalizedScore(normalizedScore)
                                .displayScore(displayScore)
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
                                .nearestFallback(false)
                                .nearestDistanceMeters(null)
                                .build())
                        .rawMetrics(RawMetricsDto.builder()
                                .foottraffic(0L).avgMonthlySales(0L)
                                .facilityCount(0).competitionCount(0)
                                .dataSourceStatus(DataSourceStatus.builder()
                                        .trafficFromApi(null)
                                        .salesFromApi(null)
                                        .facilityFromApi(null)
                                        .competitionFromApi(null)
                                        .build())
                                .build())
                        .processedScores(ProcessedScoresDto.builder()
                                .trafficScore(0.0).salesScore(0.0)
                                .competitionScore(0.0).rawScore(0.0)
                                .normalizedScore(0.2)   // (raw + 0.2)/1.0 = 0.2
                                .displayScore(20)
                                .build())
                        .finalResult(FinalResultDto.builder()
                                .locationMultiplier(FALLBACK_MULTIPLIER)
                                .grade(FALLBACK_GRADE)
                                .build())
                        .message(message)
                        .build())
                .build();
    }

    /**
     * (P0-4) 휴리스틱 기반 외부 API 응답 상태 추정.
     *
     * <p>현재는 단순히 value &gt; 0이면 API 응답으로 간주.
     * P0-1~P0-3 작업으로 SeoulApiClient/SogongongApiClient에서
     * 명시적인 ApiCallResult를 반환하도록 리팩터링하면 정확도가 올라간다.
     */
    private DataSourceStatus buildDataSourceStatus(
            long foottraffic, long avgMonthlySales, int facilityCount, int competitionCount) {
        return DataSourceStatus.builder()
                .trafficFromApi(foottraffic > 0 ? Boolean.TRUE : null)
                .salesFromApi(avgMonthlySales > 0 ? Boolean.TRUE : null)
                .facilityFromApi(facilityCount > 0 ? Boolean.TRUE : null)
                .competitionFromApi(competitionCount > 0 ? Boolean.TRUE : null)
                .build();
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
