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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Point-in-Polygon 매핑 서비스.
 *
 * - 입력: WGS84 (lat, lng)
 * - 출력: Optional<AreaMapping> — 상권 코드 + 행정동 코드
 *
 * 성능 최적화 전략:
 *  1. 첫 호출 시 DB의 모든 폴리곤을 읽어 JTS Geometry로 파싱하고 캐시한다.
 *  2. 캐시 이후 매 요청은 in-memory 순회만 수행 (약 1650+400개 polygon contains 체크).
 *  3. buffer(10)을 사용해 경계선 오차 10m 범위까지 포함 (투영 좌표계 기준).
 *
 * WKTReader는 thread-safe 하지 않으므로 캐시 빌드 시에만 사용하고 저장하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipMappingService {

    private static final double BUFFER_METERS = 10.0;

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

        // PIP 실패 시 → 가장 가까운 상권 폴리곤 탐색 (대학가, 골목상권 등 커버)
        if (areaHit == null) {
            areaHit = findNearest(getAreaCache(), point5181, 2000.0); // 반경 2km 이내
            if (areaHit != null) {
                log.info("[PIP] 상권 PIP 실패 → 최근접 상권 사용: {}", areaHit.code);
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

        return Optional.of(new AreaMapping(
                areaHit != null ? areaHit.code : null,
                areaHit != null ? areaHit.name : null,
                dongHit.code,
                dongHit.name));
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

    private PolygonEntry findNearest(List<PolygonEntry> polygons, Point point, double maxDistanceMeters) {
        PolygonEntry nearest = null;
        double minDist = Double.MAX_VALUE;
        for (PolygonEntry entry : polygons) {
            double dist = entry.geometry.distance(point);
            if (dist < minDist && dist <= maxDistanceMeters) {
                minDist = dist;
                nearest = entry;
            }
        }
        return nearest;
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
}
