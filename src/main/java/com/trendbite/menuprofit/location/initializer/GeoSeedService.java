package com.trendbite.menuprofit.location.initializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendbite.menuprofit.location.entity.AdminDong;
import com.trendbite.menuprofit.location.entity.CommercialArea;
import com.trendbite.menuprofit.location.repository.AdminDongRepository;
import com.trendbite.menuprofit.location.repository.CommercialAreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON → DB Seed 트랜잭션 처리 빈.
 *
 * <p>(P3-6) {@code GeoDataSeedInitializer}에서 분리.
 * 두 seed 메서드를 각각 독립 트랜잭션({@link Propagation#REQUIRES_NEW})으로 처리하여
 * 첫 seed가 부분 실패하더라도 두 번째 seed는 영향 받지 않게 격리한다.
 *
 * <p>또한 별도 빈({@code @Service})으로 분리해야 Spring AOP 프록시를 통해
 * {@code @Transactional}이 정상 적용된다 (self-invocation 회피).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoSeedService {

    private static final String COMMERCIAL_AREA_FILE = "geodata/commercial_area.geojson";
    private static final String ADMIN_DONG_FILE      = "geodata/admin_dong.geojson";
    private static final int    BATCH_SIZE           = 200;

    private final CommercialAreaRepository areaRepo;
    private final AdminDongRepository      dongRepo;

    private final ObjectMapper    objectMapper = new ObjectMapper();
    private final GeometryFactory gf           = new GeometryFactory();
    private final WKTWriter       wktWriter    = new WKTWriter();

    // ──────────────────────────────── commercial_area ────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedCommercialAreas() throws Exception {
        long existing = areaRepo.count();
        if (existing > 0) {
            log.info("[GeoData Seed] commercial_area 이미 적재됨 (count={}), skip", existing);
            return;
        }
        log.info("[GeoData Seed] commercial_area 적재 시작...");

        JsonNode root = readGeoJson(COMMERCIAL_AREA_FILE);
        JsonNode features = root.get("features");
        if (features == null || !features.isArray()) {
            log.warn("[GeoData Seed] commercial_area features 비어있음");
            return;
        }

        List<CommercialArea> buffer = new ArrayList<>();
        int total = 0;
        for (JsonNode feature : features) {
            JsonNode props = feature.get("properties");
            JsonNode geom  = feature.get("geometry");
            if (props == null || geom == null) continue;

            String areaCode      = readString(props, "TRDAR_CD", "trdar_cd");
            String areaName      = readString(props, "TRDAR_CD_NM", "TRDAR_CD_N", "trdar_cd_nm", "trdar_cd_n");
            String adminDongCode = readString(props, "ADSTRD_CD", "adstrd_cd");
            if (areaCode == null) continue;

            String wkt;
            try {
                wkt = geoJsonGeometryToWkt(geom);
            } catch (Exception e) {
                log.warn("[GeoData Seed] commercial_area 폴리곤 변환 실패 areaCode={}: {}", areaCode, e.getMessage());
                continue;
            }

            buffer.add(new CommercialArea(areaCode, areaName, adminDongCode, wkt, "EPSG:5181"));
            total++;

            if (buffer.size() >= BATCH_SIZE) {
                areaRepo.saveAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) areaRepo.saveAll(buffer);
        log.info("[GeoData Seed] commercial_area 적재 완료: {}건", total);
    }

    // ──────────────────────────────── admin_dong ────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedAdminDongs() throws Exception {
        long existing = dongRepo.count();
        if (existing > 0) {
            log.info("[GeoData Seed] admin_dong 이미 적재됨 (count={}), skip", existing);
            return;
        }
        log.info("[GeoData Seed] admin_dong 적재 시작...");

        JsonNode root = readGeoJson(ADMIN_DONG_FILE);
        JsonNode features = root.get("features");
        if (features == null || !features.isArray()) {
            log.warn("[GeoData Seed] admin_dong features 비어있음");
            return;
        }

        List<AdminDong> buffer = new ArrayList<>();
        int total = 0;
        for (JsonNode feature : features) {
            JsonNode props = feature.get("properties");
            JsonNode geom  = feature.get("geometry");
            if (props == null || geom == null) continue;

            // 행정동 GeoJSON 스키마는 출처마다 다양 — 가능한 필드명 모두 순차 시도
            String dongCode = readString(props,
                    "ADM_CD", "ADM_SECT_C", "ADSTRD_CD", "adm_cd", "adm_sect_c", "adstrd_cd");
            String dongName = readString(props,
                    "ADM_NM", "ADSTRD_NM", "ADSTRD_CD_NM", "adm_nm", "adstrd_nm");
            if (dongCode == null) continue;

            String wkt;
            try {
                wkt = geoJsonGeometryToWkt(geom);
            } catch (Exception e) {
                log.warn("[GeoData Seed] admin_dong 폴리곤 변환 실패 dongCode={}: {}", dongCode, e.getMessage());
                continue;
            }

            buffer.add(new AdminDong(dongCode, dongName, wkt, "EPSG:5179"));
            total++;

            if (buffer.size() >= BATCH_SIZE) {
                dongRepo.saveAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) dongRepo.saveAll(buffer);
        log.info("[GeoData Seed] admin_dong 적재 완료: {}건", total);
    }

    // ──────────────────────────────── helpers ────────────────────────────────
    private JsonNode readGeoJson(String resourcePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalStateException("GeoJSON 파일 없음: " + resourcePath);
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    /**
     * GeoJSON의 geometry 객체를 JTS Geometry로 변환 후 WKT 문자열로 반환한다.
     * 지원 타입: Polygon, MultiPolygon
     */
    private String geoJsonGeometryToWkt(JsonNode geom) {
        String type = geom.get("type").asText();
        JsonNode coords = geom.get("coordinates");
        Geometry g;
        switch (type) {
            case "Polygon":
                g = buildPolygon(coords);
                break;
            case "MultiPolygon":
                Polygon[] polys = new Polygon[coords.size()];
                for (int i = 0; i < coords.size(); i++) {
                    polys[i] = buildPolygon(coords.get(i));
                }
                g = gf.createMultiPolygon(polys);
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 geometry type: " + type);
        }
        return wktWriter.write(g);
    }

    /**
     * GeoJSON Polygon의 coordinates (2차원 ring 배열)를 JTS Polygon으로 변환.
     * rings[0] = outer ring, rings[1..] = holes
     */
    private Polygon buildPolygon(JsonNode rings) {
        if (rings == null || rings.size() == 0) {
            throw new IllegalArgumentException("빈 polygon coordinates");
        }
        LinearRing shell = buildRing(rings.get(0));
        LinearRing[] holes = new LinearRing[Math.max(rings.size() - 1, 0)];
        for (int i = 1; i < rings.size(); i++) {
            holes[i - 1] = buildRing(rings.get(i));
        }
        return gf.createPolygon(shell, holes);
    }

    private LinearRing buildRing(JsonNode ring) {
        Coordinate[] coords = new Coordinate[ring.size()];
        for (int i = 0; i < ring.size(); i++) {
            JsonNode c = ring.get(i);
            // GeoJSON 표준: [x(경도 또는 투영X), y(위도 또는 투영Y), z?]
            double x = c.get(0).asDouble();
            double y = c.get(1).asDouble();
            coords[i] = new Coordinate(x, y);
        }
        // GeoJSON 링이 닫혀있지 않으면 첫 점을 마지막에 추가해서 닫아준다.
        if (!coords[0].equals2D(coords[coords.length - 1])) {
            Coordinate[] closed = new Coordinate[coords.length + 1];
            System.arraycopy(coords, 0, closed, 0, coords.length);
            closed[coords.length] = coords[0];
            coords = closed;
        }
        return gf.createLinearRing(coords);
    }

    /** properties 객체에서 여러 후보 키 중 첫 번째로 발견되는 값을 반환 (GeoJSON 스키마 편차 대응) */
    private String readString(JsonNode props, String... keys) {
        for (String key : keys) {
            JsonNode node = props.get(key);
            if (node != null && !node.isNull()) {
                String value = node.asText();
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }
}
