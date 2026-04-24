package com.trendbite.menuprofit.location.service;

import com.trendbite.menuprofit.location.client.SeoulApiClient;
import com.trendbite.menuprofit.location.client.SogongongApiClient;
import com.trendbite.menuprofit.location.dto.AreaMapping;
import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * LocationAnalysisService 단위 테스트.
 *
 * 외부 API 클라이언트와 PIP 서비스는 모두 Mockito로 mock한다.
 * LocationCalculatorService는 실제 구현을 사용한다 (순수 계산 로직이므로).
 */
@ExtendWith(MockitoExtension.class)
class LocationAnalysisServiceTest {

    @Mock private PipMappingService   pipMappingService;
    @Mock private SeoulApiClient      seoulApiClient;
    @Mock private SogongongApiClient  sogongongApiClient;

    @InjectMocks private LocationAnalysisService service;

    // Calculator는 실제 구현으로 주입
    private final LocationCalculatorService realCalculator = new LocationCalculatorService();

    @Test
    @DisplayName("서울 외 좌표: Optional.empty → locationMultiplier=1.0 fallback 반환")
    void testAnalyze_outOfSeoul() {
        injectRealCalculator();
        given(pipMappingService.findAreaByCoord(anyDouble(), anyDouble()))
                .willReturn(Optional.empty());

        LocationAnalysisResponse res = service.analyze(
                new LocationAnalysisRequest("부산광역시 해운대구", 35.1, 129.0));

        assertThat(res.getSuccess()).isTrue();
        assertThat(res.getData().getFinalResult().getLocationMultiplier()).isEqualTo(1.0);
        assertThat(res.getData().getFinalResult().getGrade()).isEqualTo(5);
        assertThat(res.getData().getMessage()).contains("서울시");
    }

    @Test
    @DisplayName("정상 흐름: 고려대 좌표 입력 → locationMultiplier 0.8~1.2 반환")
    void testAnalyze_validKoreaUniv() {
        injectRealCalculator();
        given(pipMappingService.findAreaByCoord(anyDouble(), anyDouble()))
                .willReturn(Optional.of(new AreaMapping(
                        "1111055", "안암역_1", "1111055000", "안암동")));
        given(seoulApiClient.fetchFootTraffic(anyString())).willReturn(25_000L);
        given(seoulApiClient.fetchAvgMonthlySales(anyString())).willReturn(35_000_000L);
        given(seoulApiClient.fetchFacilityCount(anyString())).willReturn(12);
        given(sogongongApiClient.fetchCompetitionCount(anyString())).willReturn(18);

        LocationAnalysisResponse res = service.analyze(
                new LocationAnalysisRequest("서울 성북구 안암동", 37.589, 127.032));

        assertThat(res.getSuccess()).isTrue();
        Double multiplier = res.getData().getFinalResult().getLocationMultiplier();
        assertThat(multiplier).isBetween(0.8, 1.2);
        assertThat(res.getData().getLocationInfo().getAreaName()).isEqualTo("안암역_1");
        assertThat(res.getData().getRawMetrics().getFoottraffic()).isEqualTo(25_000L);
    }

    @Test
    @DisplayName("API 전부 실패(0 반환): competition이 정규화 점수 0 → 최솟값 근처 반환")
    void testAnalyze_allApisZero() {
        injectRealCalculator();
        given(pipMappingService.findAreaByCoord(anyDouble(), anyDouble()))
                .willReturn(Optional.of(new AreaMapping("X", "X", "Y", "Y")));
        given(seoulApiClient.fetchFootTraffic(anyString())).willReturn(0L);
        given(seoulApiClient.fetchAvgMonthlySales(anyString())).willReturn(0L);
        given(seoulApiClient.fetchFacilityCount(anyString())).willReturn(0);
        given(sogongongApiClient.fetchCompetitionCount(anyString())).willReturn(0);

        LocationAnalysisResponse res = service.analyze(
                new LocationAnalysisRequest("서울 어딘가", 37.5, 127.0));

        // traffic/sales/comp 모두 0 → raw = 0.4*0 + 0.4*0 - 0.2*0 = 0
        // m = 0.8 + 0.4 * ((0 + 0.2) / 1.0) = 0.88
        assertThat(res.getData().getFinalResult().getLocationMultiplier())
                .isCloseTo(0.88, org.assertj.core.data.Offset.offset(0.001));
    }

    /** @InjectMocks는 단일 생성자 주입만 지원하므로 Calculator는 리플렉션으로 주입 */
    private void injectRealCalculator() {
        try {
            var field = LocationAnalysisService.class.getDeclaredField("calculator");
            field.setAccessible(true);
            field.set(service, realCalculator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
