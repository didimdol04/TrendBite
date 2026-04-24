package com.trendbite.menuprofit.location.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 서울시 열린데이터광장 API 클라이언트 (3종).
 *
 *  1. 길단위인구-상권 (fetchFootTraffic)   — key: 상권 코드
 *  2. 추정매출-행정동 (fetchAvgMonthlySales) — key: 행정동 코드
 *  3. 집객시설-상권   (fetchFacilityCount)  — key: 상권 코드
 *
 * 호출 패턴 (서울시 OpenAPI 공통):
 *   http://openapi.seoul.go.kr:8088/{API_KEY}/json/{SERVICE}/1/5/{PARAM}
 *
 * 각 서비스의 엔드포인트명 / 응답 필드명은 실제 발급받은 API 문서와 매칭해야 함.
 * 우선 본 코드는 **일반 패턴**을 구현해 두었고, 최초 연동 시 다음 3가지 확인할 예정:
 *   - SERVICE 상수 (엔드포인트명)
 *   - 응답 JSON의 row key (예: "VwsmTrdarFlpopQq.row[0].TOT_FLPOP_CO")
 *
 * 모든 예외는 흡수하여 기본값(0)을 반환한다 (B-6 Fallback 정책).
 */
@Slf4j
@Component
public class SeoulApiClient {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";

    // 실제 엔드포인트명 — 서울시 오픈API 문서에서 최종 확인 필요
    private static final String SERVICE_TRAFFIC  = "VwsmTrdarFlpopQq";   // 서울시 상권분석서비스-길단위인구-상권
    private static final String SERVICE_SALES    = "VwsmAdstrdSelngW";   // 서울시 상권분석서비스-추정매출-행정동
    private static final String SERVICE_FACILITY = "VwsmTrdarFcltyStorW";// 서울시 상권분석서비스-집객시설-상권

    private final RestTemplate  restTemplate;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    @Value("${seoul.api.traffic-key:}")
    private String trafficKey;

    @Value("${seoul.api.sales-key:}")
    private String salesKey;

    @Value("${seoul.api.facility-key:}")
    private String facilityKey;

    public SeoulApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ──────────────────────────────── 1. 길단위인구 ────────────────────────────────
    /**
     * 상권 코드로 일 유동인구 조회.
     * 실패 시 0 반환.
     */
    public long fetchFootTraffic(String areaCode) {
        if (isKeyMissing(trafficKey) || areaCode == null) return 0L;
        String url = String.format("%s/%s/json/%s/1/5/%s",
                BASE_URL, trafficKey, SERVICE_TRAFFIC, areaCode);
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode rows = extractRows(root, SERVICE_TRAFFIC);
            if (rows == null || !rows.isArray() || rows.size() == 0) return 0L;
            JsonNode row = rows.get(0);
            // 일반적으로 TOT_FLPOP_CO 또는 유사 필드. 여러 후보명 순차 시도.
            return readLong(row, "TOT_FLPOP_CO", "FLPOP_CO", "TOT_STRD_POP", "STRD_FLPOP_CO");
        } catch (Exception e) {
            log.warn("[SeoulApi] 유동인구 조회 실패 areaCode={}: {}", areaCode, e.getMessage());
            return 0L;
        }
    }

    // ──────────────────────────────── 2. 추정매출 ────────────────────────────────
    /**
     * 행정동 코드로 월 추정매출(원) 조회.
     * 실패 시 0 반환.
     */
    public long fetchAvgMonthlySales(String dongCode) {
        if (isKeyMissing(salesKey) || dongCode == null) return 0L;
        String url = String.format("%s/%s/json/%s/1/5/%s",
                BASE_URL, salesKey, SERVICE_SALES, dongCode);
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode rows = extractRows(root, SERVICE_SALES);
            if (rows == null || !rows.isArray() || rows.size() == 0) return 0L;
            JsonNode row = rows.get(0);
            return readLong(row, "THSMON_SELNG_AMT", "MT_SELNG_AMT", "AVG_MONTH_SELNG_AMT");
        } catch (Exception e) {
            log.warn("[SeoulApi] 추정매출 조회 실패 dongCode={}: {}", dongCode, e.getMessage());
            return 0L;
        }
    }

    // ──────────────────────────────── 3. 집객시설 ────────────────────────────────
    /**
     * 상권 코드로 집객시설 수 조회 (영화관, 공원, 지하철역 등 합계).
     * 실패 시 0 반환.
     */
    public int fetchFacilityCount(String areaCode) {
        if (isKeyMissing(facilityKey) || areaCode == null) return 0;
        String url = String.format("%s/%s/json/%s/1/5/%s",
                BASE_URL, facilityKey, SERVICE_FACILITY, areaCode);
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode rows = extractRows(root, SERVICE_FACILITY);
            if (rows == null || !rows.isArray() || rows.size() == 0) return 0;
            JsonNode row = rows.get(0);
            return (int) readLong(row, "TOT_FCLTY_CO", "FCLTY_CO", "AGRDE_CO");
        } catch (Exception e) {
            log.warn("[SeoulApi] 집객시설 조회 실패 areaCode={}: {}", areaCode, e.getMessage());
            return 0;
        }
    }

    // ──────────────────────────────── helpers ────────────────────────────────
    /** 서울시 OpenAPI 응답은 {"SERVICE_NAME": {"row": [...]}} 구조 */
    private JsonNode extractRows(JsonNode root, String serviceName) {
        JsonNode serviceNode = root.get(serviceName);
        if (serviceNode == null) return null;
        return serviceNode.get("row");
    }

    private long readLong(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull()) {
                try {
                    return (long) v.asDouble();
                } catch (Exception ignored) {}
            }
        }
        return 0L;
    }

    private boolean isKeyMissing(String key) {
        return key == null || key.isBlank();
    }
}
