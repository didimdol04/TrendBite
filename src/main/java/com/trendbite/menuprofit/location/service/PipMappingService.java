package com.trendbite.menuprofit.location.service;

import com.trendbite.menuprofit.location.dto.AreaMapping;
import com.trendbite.menuprofit.location.entity.AdminDong;
import com.trendbite.menuprofit.location.entity.CommercialArea;
import com.trendbite.menuprofit.location.repository.AdminDongRepository;
import com.trendbite.menuprofit.location.repository.CommercialAreaRepository;
import com.trendbite.menuprofit.location.util.CoordinateConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Point-in-Polygon 매핑 서비스.
 *
 * - 입력: WGS84 (lat, lng)
 * - 출력: Optional&lt;AreaMapping&gt; — 상권 코드 + 행정동 코드
 *
 * 성능 최적화 전략:
 *  1. 첫 호출 시 DB의 모든 폴리곤을 읽어 JTS Geometry로 파싱하고 캐시한다.
 *  2. 캐시 이후 매 요청은 in-memory 순회만 수행 (약 1650+400개 polygon contains 체크).
 *  3. buffer(10)을 사용해 경계선 오차 10m 범위까지 포함 (투영 좌표계 기준).
 *
 * WKTReader는 thread-safe 하지 않으므로 캐시 빌드 시에만 사용하고 저장하지 않는다.
 *
 * <h3>v2 변경 사항</h3>
 * <ul>
 *   <li>(P1-3) {@link #warmUpCache()} 추가 — {@code ApplicationReadyEvent} 시점에 사전 빌드하여
 *              첫 사용자 요청의 5초 지연을 제거.</li>
 *   <li>(P1-1) {@link #findAreaByCoord(double, double)}이 fallback 여부와 거리를
 *              {@link AreaMapping#isNearestFallback()}, {@link AreaMapping#getNearestDistanceMeters()}로
 *              반환하도록 변경.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipMappingService {

    private static final double BUFFER_METERS  = 10.0;
    private static final double FALLBACK_RADIUS_METERS = 2_000.0;

    private final CommercialAreaRepository areaRepo;
    private final AdminDongRepository      dongRepo;
    private final CoordinateConverter      converter;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    /** 캐시: 상권 코드 + 이름 + Geometry (EPSG:5181) */
    private final AtomicReference<List<PolygonEntry>> areaCache = new AtomicReference<>();
    /** 캐시: 행정동 코드 + 이름 + Geometry (EPSG:5179) */
    private final AtomicReference<List<PolygonEntry>> dongCache = new AtomicReference<>();

    /**
     * 위도/경도 좌표에 해당하는 상권 + 행정동 코드를 반환한다.
     * 매핑 실패(서울 외 등) 시 Optional.empty() 반환.
     */
    public Optional<AreaMapping> findAreaByCoord(double lat, double lng) {
        // 1) 상권 코드 (EPSG:5181)
        ProjCoordinate c5181 = converter.toEpsg5181(lat, lng);
        Point point5181 = geometryFactory.createPoint(new Coordinate(c5181.x, c5181.y));

        // PIP 우선 시도
        PolygonEntry areaHit = findContaining(getAreaCache(), point5181);
        boolean nearestFallback = false;
        Double  nearestDistance = null;

        // PIP 실패 시 → 가장 가까운 상권 폴리곤 탐색 (대학가, 골목상권 등 커버)
        if (areaHit == null) {
            NearestMatch match = findNearest(getAreaCache(), point5181, FALLBACK_RADIUS_METERS);
            if (match != null) {
                areaHit         = match.entry;
                nearestFallback = true;
                nearestDistance = match.distanceMeters;
                log.info("[PIP] 상권 PIP 실패 → 최근접 상권 사용: {} (거리 {}m)",
                        areaHit.code, String.format("%.1f", nearestDistance));
            }
        }

        // 2) 행정동 코드 (EPSG:5179)
        ProjCoordinate c5179 = converter.toEpsg5179(lat, lng);
        Point point5179 = geometryFactory.createPoint(new Coordinate(c5179.x, c5179.y));
        PolygonEntry dongHit = findContaining(getDongCache(), point5179);

        if (dongHit == null) {
            log.debug("[PIP] 매핑 실패 - 행정동 없음 lat={}, lng={}", lat, lng);
            return Optional.empty();
        }

        // (P1-1) fallback 메타데이터 포함하여 반환
        return Optional.of(AreaMapping.builder()
                .areaCode(areaHit != null ? areaHit.code : null)
                .areaName(areaHit != null ? areaHit.name : null)
                .dongCode(dongHit.code)
                .dongName(dongHit.name)
                .nearestFallback(nearestFallback)
                .nearestDistanceMeters(nearestDistance)
                .build());
    }

    // ──────────────────────────────── (P1-3) Application 기동 시 사전 빌드 ────────────────────────────────

    /**
     * Spring 컨텍스트 기동이 끝난 직후(웹서버 시작 후) PIP 캐시를 사전 빌드한다.
     * 사용자의 첫 요청에서 5초 지연이 발생하지 않도록 미리 워밍업.
     *
     * <p>주의: {@code @PostConstruct} 대신 {@code ApplicationReadyEvent}를 사용하는 이유는,
     * GeoDataSeedInitializer({@code CommandLineRunner})가 GeoJSON → DB 적재를 완료한 뒤에
     * 캐시를 빌드해야 하기 때문이다. ApplicationReadyEvent는 모든 CommandLineRunner 실행 후 발생한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        long start = System.currentTimeMillis();
        log.info("[PIP Cache] 사전 빌드 시작 (ApplicationReadyEvent)");
        try {
            getAreaCache();
            getDongCache();
            log.info("[PIP Cache] 사전 빌드 완료: {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 캐시 빌드 실패는 서비스 자체를 막지 않음 — 첫 사용자 요청 시 lazy 빌드로 fallback
            log.error("[PIP Cache] 사전 빌드 실패 (lazy 빌드로 fallback): {}", e.getMessage(), e);
        }
    }

    // ──────────────────────────────── cache build ────────────────────────────────
    private List<PolygonEntry> getAreaCache() {
        List<PolygonEntry> cached = areaCache.get();
        if (cached != null) return cached;
        synchronized (this) {
            cached = areaCache.get();
            if (cached != null) return cached;
            List<PolygonEntry> built = buildAreaCache();
            areaCache.set(built);
            return built;
        }
    }

    private List<PolygonEntry> getDongCache() {
        List<PolygonEntry> cached = dongCache.get();
        if (cached != null) return cached;
        synchronized (this) {
            cached = dongCache.get();
            if (cached != null) return cached;
            List<PolygonEntry> built = buildDongCache();
            dongCache.set(built);
            return built;
        }
    }

    private List<PolygonEntry> buildAreaCache() {
        log.info("[PIP Cache] 상권 Geometry 캐시 빌드 시작");
        WKTReader reader = new WKTReader(geometryFactory);
        List<PolygonEntry> result = new ArrayList<>();
        for (CommercialArea area : areaRepo.findAll()) {
            try {
                Geometry g = reader.read(area.getPolygonWkt());
                result.add(new PolygonEntry(area.getAreaCode(), area.getAreaName(), g.buffer(BUFFER_METERS)));
            } catch (ParseException e) {
                log.warn("[PIP Cache] 상권 WKT 파싱 실패 areaCode={}: {}", area.getAreaCode(), e.getMessage());
            }
        }
        log.info("[PIP Cache] 상권 Geometry 캐시 완료: {}건", result.size());
        return result;
    }

    private List<PolygonEntry> buildDongCache() {
        log.info("[PIP Cache] 행정동 Geometry 캐시 빌드 시작");
        WKTReader reader = new WKTReader(geometryFactory);
        List<PolygonEntry> result = new ArrayList<>();
        for (AdminDong dong : dongRepo.findAll()) {
            try {
                Geometry g = reader.read(dong.getPolygonWkt());
                result.add(new PolygonEntry(dong.getDongCode(), dong.getDongName(), g.buffer(BUFFER_METERS)));
            } catch (ParseException e) {
                log.warn("[PIP Cache] 행정동 WKT 파싱 실패 dongCode={}: {}", dong.getDongCode(), e.getMessage());
            }
        }
        log.info("[PIP Cache] 행정동 Geometry 캐시 완료: {}건", result.size());
        return result;
    }

    private PolygonEntry findContaining(List<PolygonEntry> polygons, Point point) {
        for (PolygonEntry entry : polygons) {
            if (entry.geometry.contains(point)) return entry;
        }
        return null;
    }

    /**
     * (P1-1) {@code maxDistanceMeters} 이내에서 가장 가까운 폴리곤 탐색.
     * 결과가 있으면 거리와 함께 반환.
     */
    private NearestMatch findNearest(List<PolygonEntry> polygons, Point point, double maxDistanceMeters) {
        PolygonEntry nearest = null;
        double minDist = Double.MAX_VALUE;
        for (PolygonEntry entry : polygons) {
            double dist = entry.geometry.distance(point);
            if (dist < minDist && dist <= maxDistanceMeters) {
                minDist = dist;
                nearest = entry;
            }
        }
        return nearest != null ? new NearestMatch(nearest, minDist) : null;
    }

    // ──────────────────────────────── inner VO ────────────────────────────────
    private static class PolygonEntry {
        final String   code;
        final String   name;
        final Geometry geometry;
        PolygonEntry(String code, String name, Geometry geometry) {
            this.code = code;
            this.name = name;
            this.geometry = geometry;
        }
    }

    /** (P1-1) 최근접 폴리곤 + 거리 정보를 함께 반환하기 위한 내부 VO. */
    private static class NearestMatch {
        final PolygonEntry entry;
        final double distanceMeters;
        NearestMatch(PolygonEntry entry, double distanceMeters) {
            this.entry = entry;
            this.distanceMeters = distanceMeters;
        }
    }
}
