package com.trendbite.menuprofit.location.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.ProjCoordinate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CoordinateConverter 단위 테스트.
 *
 * proj4j EPSG 의존성만 있으면 Spring 컨텍스트 없이 검증 가능.
 * 기대값은 공식 좌표계 변환 결과의 근사치를 사용한다.
 */
class CoordinateConverterTest {

    private CoordinateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CoordinateConverter();
        converter.init();   // @PostConstruct를 수동 호출
    }

    @Test
    @DisplayName("고려대 좌표 WGS84 → EPSG:5181 변환 시 대한민국 중부원점 기반 좌표 반환")
    void testToEpsg5181_koreaUniv() {
        // 고려대학교 근처 (lat=37.589, lng=127.032)
        ProjCoordinate result = converter.toEpsg5181(37.589, 127.032);

        // EPSG:5181 False Easting=200000, False Northing=500000, 중부원점(127E, 38N)
        // 고려대: 원점보다 약 3km 동쪽 → x ≈ 203000
        //         원점보다 약 45km 남쪽 → y ≈ 454000
        assertThat(result.x).isBetween(195_000.0, 215_000.0);
        assertThat(result.y).isBetween(440_000.0, 470_000.0);  // 하한선 455000 → 440000으로 수정
    }

    @Test
    @DisplayName("WGS84 → EPSG:5179 변환도 정상 동작")
    void testToEpsg5179_koreaUniv() {
        ProjCoordinate result = converter.toEpsg5179(37.589, 127.032);
        // EPSG:5179는 대한민국 통합원점 (UTM-K). 서울 중심부는 대략 x=95만±5만, y=195만±5만.
        assertThat(result.x).isGreaterThan(900_000.0);
        assertThat(result.y).isGreaterThan(1_900_000.0);
    }

    @Test
    @DisplayName("서울 밖 좌표(부산)도 예외 없이 변환됨 (좌표값만 다를 뿐)")
    void testToEpsg5181_outOfSeoul() {
        // 부산 (lat=35.1, lng=129.0)
        ProjCoordinate result = converter.toEpsg5181(35.1, 129.0);
        assertThat(result).isNotNull();
        // x는 매우 커지고 y는 음수일 수 있다 — 예외만 안 나면 OK
    }
}
