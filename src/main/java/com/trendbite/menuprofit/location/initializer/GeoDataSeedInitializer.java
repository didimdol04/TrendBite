package com.trendbite.menuprofit.location.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring Boot 기동 시 resources/geodata/ 폴더의 GeoJSON 파일을 읽어
 * commercial_area / admin_dong 테이블에 INSERT 한다.
 *
 * v2.0 방식 (v1.0의 SeoulApiClient HTTP 호출 대체):
 *  - commercial_area.geojson → CommercialArea 엔티티 → EPSG:5181
 *  - admin_dong.geojson      → AdminDong 엔티티       → EPSG:5179
 *
 * 이미 적재된 경우 skip (테이블에 데이터 있으면 재실행하지 않음).
 *
 * v2 (P3-6): 트랜잭션 분리
 * 이전 버전은 본 클래스 내에서 두 seed를 하나의 {@code @Transactional}로 처리했으나,
 * 첫 seed가 부분 실패(WKT 파싱 등)로 트랜잭션 mark-as-rollback 상태가 되면 두 번째 seed의
 * {@code saveAll}에서 {@code UnexpectedRollbackException}이 발생할 위험이 있었다.
 *
 * 이를 해결하기 위해 실제 트랜잭션 처리는 {@link GeoSeedService}로 위임하고,
 * 두 seed 메서드는 각각 {@code REQUIRES_NEW}로 독립 실행되도록 변경했다.
 * 본 클래스는 단순 {@link CommandLineRunner} 진입점 역할만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeoDataSeedInitializer implements CommandLineRunner {

    private final GeoSeedService geoSeedService;

    @Override
    public void run(String... args) {
        try {
            geoSeedService.seedCommercialAreas();
        } catch (Exception e) {
            log.error("[GeoData Seed] commercial_area 적재 실패: {}", e.getMessage(), e);
        }
        try {
            geoSeedService.seedAdminDongs();
        } catch (Exception e) {
            log.error("[GeoData Seed] admin_dong 적재 실패: {}", e.getMessage(), e);
        }
    }
}
