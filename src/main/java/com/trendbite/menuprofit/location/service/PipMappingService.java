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
 *  3. buffer(10)을 사용해 경계선 오차 10m 범위까지 포함.
 *
 * <h3>v3 변경 사항 (round 2)</h3>
 * <ul>
 *   <li>(P3-1) {@link #invalidateCache()}, {@link #invalidateAndRebuild()} 추가 —
 *              GeoJSON 데이터 갱신 시 서버 재기동 없이 캐시 새로고침 가능
 *              ({@link com.trendbite.menuprofit.location.controller.LocationAdminController}에서 노출).</li>
 * </ul>
 *
 * <h3>v2 변경 사항</h3>
 * <ul>
 *   <li>(P1-3) {@link #warmUpCache()} — ApplicationReadyEvent 시점 사전 빌드</li>
 *   <li>(P1-1) {@link AreaMapping#isNearestFallback()}, distance 정보 반환</li>
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

    private final AtomicReference<List<PolygonEntry>> areaCache = new AtomicReference<>();
    private final AtomicReference<List<PolygonEntry>> dongCache = new AtomicReference<>();

    /**
     * 위도/경도 좌표에 해당하는 상권 + 행정동 코드를 반환한다.
     * 매핑 실패(서울 외 등) 시 Optional.empty() 반환.
     */
    public Optional<AreaMapping> findAreaByCoord(double lat, double lng) {
        ProjCoordinate c5181 = converter.toEpsg5181(lat, lng);
        Point point5181 = geometryFactory.createPoint(new Coordinate(c5181.x, c5181.y));

        PolygonEntry areaHit = findContaining(getAreaCache(), point5181);
        boolean nearestFallback = false;
        Double  nearestDistance = null;

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

        ProjCoordinate c5179 = converter.toEpsg5179(lat, lng);
        Point point5179 = geometryFactory.createPoint(new Coordinate(c5179.x, c5179.y));
        PolygonEntry dongHit = findContaining(getDongCache(), point5179);

        if (dongHit == null) {
            log.debug("[PIP] 매핑 실패 - 행정동 없음 lat={}, lng={}", lat, lng);
            return Optional.empty();
        }

        return Optional.of(AreaMapping.builder()
                .areaCode(areaHit != null ? areaHit.code : null)
                .areaName(areaHit != null ? areaHit.name : null)
                .dongCode(dongHit.code)
                .dongName(dongHit.name)
                .nearestFallback(nearestFallback)
                .nearestDistanceMeters(nearestDistance)
                .build());
    }

    // ──────────────────────────────── (P1-3) 사전 빌드 ────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        long start = System.currentTimeMillis();
        log.info("[PIP Cache] 사전 빌드 시작 (ApplicationReadyEvent)");
        try {
            getAreaCache();
            getDongCache();
            log.info("[PIP Cache] 사전 빌드 완료: {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[PIP Cache] 사전 빌드 실패 (lazy 빌드로 fallback): {}", e.getMessage(), e);
        }
    }

    // ──────────────────────────────── (P3-1) 캐시 무효화/재빌드 ────────────────────────────────

    /**
     * (P3-1) 캐시 무효화 — GeoJSON 데이터가 갱신됐을 때 호출.
     * 다음 사용자 요청 시 lazy 빌드된다 (~5초 지연 발생).
     * 즉시 재빌드가 필요하면 {@link #invalidateAndRebuild()} 사용.
     */
    public void invalidateCache() {
        log.info("[PIP Cache] 캐시 무효화 요청");
        areaCache.set(null);
        dongCache.set(null);
    }

    /**
     * (P3-1) 캐시 무효화 + 즉시 재빌드.
     * 운영 환경에서 GeoJSON 갱신 후 사용자 응답 시간 영향 없이 새 데이터 적용.
     */
    public void invalidateAndRebuild() {
        invalidateCache();
        warmUpCache();
    }

    // ──────────────────────────────── cache build (내부) ────────────────────────────────

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

    private static class NearestMatch {
        final PolygonEntry entry;
        final double distanceMeters;
        NearestMatch(PolygonEntry entry, double distanceMeters) {
            this.entry = entry;
            this.distanceMeters = distanceMeters;
        }
    }
}
