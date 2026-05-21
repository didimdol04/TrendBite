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
import com.trendbite.menuprofit.location.entity.SaleStat;
import com.trendbite.menuprofit.location.repository.SaleStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 입지 분석 메인 오케스트레이션 서비스.
 *
 * 흐름:
 *   (1) PipMappingService   → 상권 코드 + 행정동 코드 + fallback 메타데이터
 *   (2) SeoulApiClient x2   → 유동인구 + 집객시설 수
 *   (3) SaleStatRepository  → 추정매출 (DB 사전 적재 데이터)
 *   (4) SogongongApiClient  → 경쟁 점포 수
 *   (5) LocationCalculator  → locationMultiplier + grade
 *   (6) Response DTO 조립
 *
 * v4 변경 사항 (추정매출 DB 전환)
 * 
 *   기존 {@code seoulApiClient.fetchAvgMonthlySales(dongCode)} 실시간 API 호출 →
 *       {@code saleStatRepository.findById(dongCode)} DB 조회로 변경.
 *       서울시 추정매출 API가 47만 건 bulk 구조라 실시간 필터 불가 문제 해결.
 *   서버 최초 기동 시 {@link com.trendbite.menuprofit.location.initializer.SaleStatInitializer}가
 *       백그라운드에서 전체 적재. 적재 완료 전 요청은 0원으로 응답.
 *   {@link DataSourceStatus#salesFromApi}가 이제 DB 적재 여부를 표시 (API 직접 호출이 아님).
 * 
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
    private final SaleStatRepository        saleStatRepository;  // ← 추가

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

        // (2) 실시간 외부 API 호출 (각각 실패 시 0 반환)
        long   foottraffic     = seoulApiClient.fetchFootTraffic(mapping.getAreaCode());
        int    facilityCount   = seoulApiClient.fetchFacilityCount(mapping.getAreaCode());
        int    competitionCnt  = sogongongApiClient.fetchCompetitionCount(mapping.getDongCode());

        // (3) 추정매출 — DB 사전 적재 데이터 조회 (실시간 API 호출 대신)
        //     백그라운드 적재 완료 전이면 Optional.empty → 0L
        long avgMonthlySales = saleStatRepository.findById(mapping.getDongCode())
                .map(SaleStat::getFoodMonthlySales)
                .orElse(0L);

        LocationRawData raw = LocationRawData.builder()
                .foottraffic(foottraffic)
                .avgMonthlySales(avgMonthlySales)
                .facilityCount(facilityCount)
                .competitionCount(competitionCnt)
                .build();

        // (4) 계수 산출
        double trafficScore     = calculator.normalizeTraffic(foottraffic);
        double salesScore       = calculator.normalizeSales(avgMonthlySales);
        double competitionScore = calculator.normalizeCompetition(competitionCnt);
        double rawScore   = calculator.calculateRawScore(trafficScore, salesScore, competitionScore);
        double multiplier = calculator.calculateFromScores(trafficScore, salesScore, competitionScore);
        int    grade      = calculator.calculateGrade(multiplier);

        // (P2-2) 표시용 정규화 점수
        double normalizedScore = round3((rawScore + 0.2) / 1.0);
        int    displayScore    = (int) Math.round(normalizedScore * 100);

        // (P0-4) 외부 API/DB 정상 응답 여부
        DataSourceStatus dataSourceStatus = buildDataSourceStatus(
                foottraffic, avgMonthlySales, facilityCount, competitionCnt);

        // (P3-7) 정상 흐름 일괄 로그
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

        // (5) Response 조립
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
                                .normalizedScore(0.2)
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
