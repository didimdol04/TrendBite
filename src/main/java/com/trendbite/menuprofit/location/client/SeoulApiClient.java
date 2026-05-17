package com.trendbite.menuprofit.location.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;

/**
 * 서울시 열린데이터광장 API 클라이언트 (3종).
 *
 *  1. 길단위인구-상권 (fetchFootTraffic)   — 상권 코드(TRDAR_CD)로 필터
 *  2. 추정매출-행정동 (fetchAvgMonthlySales) — 행정동 코드(ADSTRD_CD)로 필터
 *  3. 집객시설-상권   (fetchFacilityCount)  — 상권 코드(TRDAR_CD)로 필터
 *
 * <h3>v3 변경 사항 (round 2)</h3>
 * <ul>
 *   <li>(P0-1) {@link #fetchFootTraffic(String)} - 진단용 첫 row 필드 덤프 + 4개 후보 필드 순차 시도</li>
 *   <li>(P0-2) {@link #fetchAvgMonthlySales(String)} - 페이지네이션 추가 (최대 5,000건까지 조회)</li>
 *   <li>(P0-2b) {@link #fetchAvgMonthlySales(String)} - {@link #isFoodIndustry(String)} 필터 실제 호출
 *       (기존에는 정의만 되고 미사용이라 모든 업종 매출이 합산되던 문제 해결)</li>
 *   <li>(P4-2) 생성자에 {@code @Qualifier("restTemplate")} 명시
 *       — 트렌드 도메인의 fastApiRestTemplate 빈과 모호성 제거 (timeout 5/10s 보장)</li>
 * </ul>
 *
 * 모든 예외는 흡수하여 기본값(0)을 반환한다 (B-6 Fallback 정책).
 */
@Slf4j
@Component
public class SeoulApiClient {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";

    // 명세서 일치 (서비스명 SERVICE 인자)
    private static final String SERVICE_TRAFFIC  = "VwsmTrdarFlpopQq";   // 길단위인구-상권
    private static final String SERVICE_SALES    = "VwsmAdstrdSelngW";   // 추정매출-행정동
    private static final String SERVICE_FACILITY = "VwsmTrdarFcltyQq";   // 집객시설-상권

    // 1회 페이지 사이즈 (명세서 최대 1000)
    private static final int PAGE_SIZE = 1000;
    /** (P0-2) 추정매출 페이지네이션 최대 페이지 수 — 1,000 × 5 = 5,000 row */
    private static final int SALES_MAX_PAGES = 5;

    // (P0-1) 유동인구 후보 필드명 (서울시 명세서 버전에 따라 다를 수 있어 순차 시도)
    private static final String[] FOOT_TRAFFIC_FIELDS = {
            "TOT_FLPOP_CO", "FLPOP_CO", "TOT_STRD_POP_CO", "STRD_FLPOP_CO"
    };

    private final RestTemplate  restTemplate;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    @Value("${seoul.api.traffic-key:}")
    private String trafficKey;

    @Value("${seoul.api.sales-key:}")
    private String salesKey;

    @Value("${seoul.api.facility-key:}")
    private String facilityKey;

    /**
     * (P4-2) {@code @Qualifier("restTemplate")}를 명시하여 location 도메인의 RestTemplate 빈을
     * 정확히 주입받는다. 트렌드 도메인의 {@code fastApiRestTemplate}(timeout 30s + X-API-Key 인터셉터)이
     * 잘못 주입되는 모호성 위험을 제거.
     */
    public SeoulApiClient(@Qualifier("restTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ──────────────────────────────── 1. 길단위인구 ────────────────────────────────

    /**
     * 상권 코드로 일 유동인구 조회.
     * <p>
     * (P0-1) 4개 후보 필드를 순차 시도하여 명세서 버전 차이에 대응.
     * 첫 매칭 row의 모든 필드명을 디버그 로그로 덤프하여 운영 환경에서도 응답 구조 파악 가능.
     */
    public long fetchFootTraffic(String areaCode) {
        if (isKeyMissing(trafficKey) || areaCode == null) return 0L;
        String url = buildUrl(trafficKey, SERVICE_TRAFFIC);
        try {
            JsonNode rows = callAndExtractRows(url, SERVICE_TRAFFIC);
            if (rows == null) return 0L;

            JsonNode latest = findLatestByCode(rows, "TRDAR_CD", areaCode);
            if (latest == null) {
                log.info("[SeoulApi] 길단위인구 매칭 없음 areaCode={}", areaCode);
                return 0L;
            }

            // (P0-1) 진단 로그 — 첫 매칭 row 필드 덤프
            logFirstRowFields(latest, "FootTraffic");

            // (P0-1) 후보 필드 순차 시도
            for (String candidate : FOOT_TRAFFIC_FIELDS) {
                JsonNode v = latest.get(candidate);
                if (v != null && !v.isNull()) {
                    long value = readLong(latest, candidate);
                    log.info("[SeoulApi] 유동인구 areaCode={} 필드={} 값={}", areaCode, candidate, value);
                    return value;
                }
            }
            log.warn("[SeoulApi] 유동인구 응답에 알려진 후보 필드 모두 없음 areaCode={}", areaCode);
            return 0L;
        } catch (Exception e) {
            log.warn("[SeoulApi] 유동인구 조회 실패 areaCode={}: {}", areaCode, e.getMessage());
            return 0L;
        }
    }

    // ──────────────────────────────── 2. 추정매출 ────────────────────────────────

    /**
     * 행정동 코드로 월 추정매출(원) 조회.
     * <p>
     * 같은 행정동 + 같은 분기에는 업종(SVC_INDUTY_CD)별로 여러 row가 있으므로
     * 외식업 관련 업종 row들의 THSMON_SELNG_AMT를 합산한다.
     * <p>
     * (P0-2) 페이지네이션 적용 — 1~1000, 1001~2000, ... 최대 5,000건까지 조회.
     * 이전에는 1~1000만 조회하여 가나다순 후순위 행정동(예: 안암동)이 누락되는 문제가 있었음.
     * <p>
     * (P0-2b) {@link #isFoodIndustry(String)} 필터 호출을 추가하여 외식업 매출만 합산.
     * 이전에는 행정동+분기만 매칭해서 편의점/학원/미용실 매출까지 모두 합산되던 버그.
     */
    public long fetchAvgMonthlySales(String dongCode) {
        if (isKeyMissing(salesKey) || dongCode == null) return 0L;

        long totalSales = 0L;
        int  matched = 0;
        String latestQuarter = null;
        boolean firstFoodRowLogged = false;

        // (P0-2) 페이지네이션 루프
        for (int page = 1; page <= SALES_MAX_PAGES; page++) {
            int start = (page - 1) * PAGE_SIZE + 1;
            int end   = page * PAGE_SIZE;
            String url = String.format("%s/%s/json/%s/%d/%d",
                    BASE_URL, salesKey, SERVICE_SALES, start, end);

            try {
                JsonNode rows = callAndExtractRows(url, SERVICE_SALES);
                if (rows == null || rows.size() == 0) break;

                // 첫 페이지에서 최신 분기 결정 (전체 데이터셋 기준)
                if (latestQuarter == null) {
                    latestQuarter = findLatestQuarter(rows);
                    log.info("[SeoulApi] 추정매출 페이지네이션 시작 dongCode={} latestQuarter={}",
                            dongCode, latestQuarter);
                }

                for (JsonNode row : rows) {
                    String rowDong    = textOf(row, "ADSTRD_CD");
                    String rowQuarter = textOf(row, "STDR_YYQU_CD");
                    String indutyName = textOf(row, "SVC_INDUTY_CD_NM");

                    if (!dongCode.equals(rowDong)) continue;
                    if (latestQuarter != null && !latestQuarter.equals(rowQuarter)) continue;
                    // (P0-2b) 외식업 필터 — 외식업 row만 합산
                    if (!isFoodIndustry(indutyName)) continue;

                    // 첫 외식업 row 디버그 로그 (실제 필드값 확인용)
                    if (!firstFoodRowLogged) {
                        log.info("[SeoulApi] 추정매출 첫 외식업 매칭 row - SVC_INDUTY_CD={}, SVC_INDUTY_CD_NM={}",
                                textOf(row, "SVC_INDUTY_CD"), indutyName);
                        firstFoodRowLogged = true;
                    }
                    totalSales += readLong(row, "THSMON_SELNG_AMT");
                    matched++;
                }

                // 마지막 페이지 도달 시 종료
                if (rows.size() < PAGE_SIZE) break;
            } catch (Exception e) {
                log.warn("[SeoulApi] 추정매출 페이지 {}~{} 실패 dongCode={}: {}",
                        start, end, dongCode, e.getMessage());
                break;
            }
        }

        log.info("[SeoulApi] 추정매출 dongCode={} quarter={} 외식업 {}건 합산 = {}원",
                dongCode, latestQuarter, matched, totalSales);
        return totalSales;
    }

    // ──────────────────────────────── 3. 집객시설 ────────────────────────────────

    /**
     * 상권 코드로 집객시설 수 조회.
     * 응답 row 중 TRDAR_CD가 일치하고 STDR_YYQU_CD가 최신인 것의 VIATR_FCLTY_CO.
     */
    public int fetchFacilityCount(String areaCode) {
        if (isKeyMissing(facilityKey) || areaCode == null) return 0;
        String url = buildUrl(facilityKey, SERVICE_FACILITY);
        try {
            JsonNode rows = callAndExtractRows(url, SERVICE_FACILITY);
            if (rows == null) return 0;

            JsonNode latest = findLatestByCode(rows, "TRDAR_CD", areaCode);
            if (latest == null) {
                log.info("[SeoulApi] 집객시설 매칭 없음 areaCode={}", areaCode);
                return 0;
            }
            return (int) readLong(latest, "VIATR_FCLTY_CO");
        } catch (Exception e) {
            log.warn("[SeoulApi] 집객시설 조회 실패 areaCode={}: {}", areaCode, e.getMessage());
            return 0;
        }
    }

    // ──────────────────────────────── helpers ────────────────────────────────

    private String buildUrl(String key, String service) {
        // 명세: /{KEY}/json/{SERVICE}/{START_INDEX}/{END_INDEX}
        return String.format("%s/%s/json/%s/1/%d", BASE_URL, key, service, PAGE_SIZE);
    }

    private JsonNode callAndExtractRows(String url, String serviceName) throws Exception {
        String response = restTemplate.getForObject(url, String.class);
        if (response == null) return null;
        JsonNode root = objectMapper.readTree(response);
        JsonNode serviceNode = root.get(serviceName);
        if (serviceNode == null) {
            log.warn("[SeoulApi] 응답에 서비스 키 없음: {}", serviceName);
            return null;
        }
        // 결과 코드 체크 (RESULT.CODE != "INFO-000"이면 에러)
        JsonNode result = serviceNode.get("RESULT");
        if (result != null) {
            String code = textOf(result, "CODE");
            if (code != null && !"INFO-000".equals(code)) {
                log.warn("[SeoulApi] {} API 에러: {} - {}",
                        serviceName, code, textOf(result, "MESSAGE"));
                return null;
            }
        }
        JsonNode rows = serviceNode.get("row");
        if (rows == null || !rows.isArray() || rows.size() == 0) return null;
        return rows;
    }

    /**
     * (P0-1) 진단용 — row 한 건의 모든 필드명/값을 INFO 레벨로 출력.
     * 운영 환경에서 응답 구조가 명세와 다를 때 빠르게 식별 가능.
     */
    private void logFirstRowFields(JsonNode row, String serviceName) {
        if (row == null) return;
        StringBuilder sb = new StringBuilder();
        Iterator<String> fields = row.fieldNames();
        while (fields.hasNext()) {
            String f = fields.next();
            sb.append(f).append("=").append(textOf(row, f)).append(", ");
        }
        log.info("[SeoulApi] [{}] 첫 row 필드 덤프: {}", serviceName, sb);
    }

    /**
     * row 배열에서 codeField 값이 targetCode와 일치하는 row 중
     * STDR_YYQU_CD 가 가장 큰(최신) 것을 반환.
     */
    private JsonNode findLatestByCode(JsonNode rows, String codeField, String targetCode) {
        JsonNode latest = null;
        String latestQuarter = null;
        for (JsonNode row : rows) {
            String rowCode = textOf(row, codeField);
            if (!targetCode.equals(rowCode)) continue;
            String quarter = textOf(row, "STDR_YYQU_CD");
            if (latestQuarter == null || (quarter != null && quarter.compareTo(latestQuarter) > 0)) {
                latest = row;
                latestQuarter = quarter;
            }
        }
        return latest;
    }

    /** 응답 전체에서 가장 큰 STDR_YYQU_CD 값 반환. */
    private String findLatestQuarter(JsonNode rows) {
        String latest = null;
        for (JsonNode row : rows) {
            String q = textOf(row, "STDR_YYQU_CD");
            if (q == null) continue;
            if (latest == null || q.compareTo(latest) > 0) {
                latest = q;
            }
        }
        return latest;
    }

    /**
     * 외식업 업종명 판정 (서울시 SVC_INDUTY_CD_NM 기준).
     * 명세에 정확한 코드표가 없어 한글명 키워드로 식별한다.
     * (P0-2b)에서 실제로 호출되도록 활성화됨.
     */
    private boolean isFoodIndustry(String indutyName) {
        if (indutyName == null) return false;
        String n = indutyName;
        return n.contains("한식") || n.contains("중식") || n.contains("일식") || n.contains("양식")
                || n.contains("분식") || n.contains("카페") || n.contains("제과")
                || n.contains("패스트푸드") || n.contains("치킨") || n.contains("호프")
                || n.contains("주점") || n.contains("커피") || n.contains("음식")
                || n.contains("베이커리") || n.contains("아이스크림") || n.contains("샤브")
                || n.contains("피자") || n.contains("뷔페");
    }

    private String textOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private long readLong(JsonNode node, String key) {
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) return 0L;
        try {
            return (long) v.asDouble();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private boolean isKeyMissing(String key) {
        return key == null || key.isBlank();
    }
}
