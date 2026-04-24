package com.trendbite.menuprofit.location.integration;

import com.trendbite.menuprofit.location.dto.AreaMapping;
import com.trendbite.menuprofit.location.repository.AdminDongRepository;
import com.trendbite.menuprofit.location.repository.CommercialAreaRepository;
import com.trendbite.menuprofit.location.service.PipMappingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PIP 매핑 end-to-end 통합 테스트.
 *
 * ⚠ 이 테스트는 다음 조건이 모두 만족될 때만 실행된다:
 *    1. MySQL이 실행 중 + menuprofit DB 존재
 *    2. commercial_area / admin_dong 테이블에 Seed 완료 (GeoDataSeedInitializer)
 *    3. JVM 시스템 프로퍼티 `-Drun.integration.tests=true`
 *
 *  실행 방법:
 *    ./gradlew test -Drun.integration.tests=true
 *
 *  평상시 `./gradlew test` 에서는 자동 skip 되어 빌드가 막히지 않는다.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "run.integration.tests", matches = "true")
class PipMappingIntegrationTest {

    @Autowired private PipMappingService        pipMappingService;
    @Autowired private CommercialAreaRepository areaRepo;
    @Autowired private AdminDongRepository      dongRepo;

    @Test
    @DisplayName("고려대 좌표 PIP 매핑 — 상권 코드 + 행정동 코드 모두 반환")
    void testPip_koreaUniv() {
        // Seed 선행 조건 확인
        assertThat(areaRepo.count()).isGreaterThan(0);
        assertThat(dongRepo.count()).isGreaterThan(0);

        // 고려대학교 정문 부근
        Optional<AreaMapping> result = pipMappingService.findAreaByCoord(37.589, 127.032);

        assertThat(result).isPresent();
        AreaMapping mapping = result.get();
        assertThat(mapping.getAreaCode()).isNotBlank();
        assertThat(mapping.getDongCode()).isNotBlank();
        // 예: 상권명에 "안암" 포함 가능 (GeoJSON 스키마에 따라 다름)
    }

    @Test
    @DisplayName("서울 외 좌표(부산) PIP 매핑 — Optional.empty 반환")
    void testPip_outOfSeoul() {
        Optional<AreaMapping> result = pipMappingService.findAreaByCoord(35.159, 129.160);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("강남역 좌표 PIP 매핑 — 매핑 성공")
    void testPip_gangnam() {
        Optional<AreaMapping> result = pipMappingService.findAreaByCoord(37.4979, 127.0276);
        assertThat(result).isPresent();
    }
}
