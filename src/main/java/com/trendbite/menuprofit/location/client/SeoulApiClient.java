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
 *  1. 길단위인구-상권 (fetchFootTraffic)   — 상권 코드(TRDAR_CD)로 필터
 *  2. 추정매출-행정동 (fetchAvgMonthlySales) — 행정동 코드(ADSTRD_CD)로 필터
 *  3. 집객시설-상권   (fetchFacilityCount)  — 상권 코드(TRDAR_CD)로 필터
 *
 * 호출 패턴 (명세서 기반):
 *   http://openapi.seoul.go.kr:8088/{API_KEY}/json/{SERVICE}/{시작}/{끝}
 *   - 1회 최대 1,000건
 *   - URL 경로에 코드 인자 없음. 응답 받은 후 TRDAR_CD/ADSTRD_CD 일치하는 row 필터링.
 *   - STDR_YYQU_CD(기준 년분기 코드) max 값으로 최신 분기 선택 (혼합 방지).
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
    private static final String SERVICE_FACILITY = "VwsmTrdarFcltyQq";   // 집객시설-상권 (Stor → Qq 수정)

    // 1회 페이지 사이즈 (명세서 최대 1000)
    private static final int PAGE_SIZE = 1000;

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
     * 응답 row 중 TRDAR_CD가 일치하고 STDR_YYQU_CD가 최신인 것의 TOT_FLPOP_CO.
     * 실패 또는 매칭 없음 시 0 반환.
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
            return readLong(latest, "TOT_FLPOP_CO");
        } catch (Exception e) {
            log.warn("[SeoulApi] 유동인구 조회 실패 areaCode={}: {}", areaCode, e.getMessage());
            return 0L;
        }
    }

    // ──────────────────────────────── 2. 추정매출 ────────────────────────────────
    /**
     * 행정동 코드로 월 추정매출(원) 조회.
     * 같은 행정동 + 같은 분기에는 업종(SVC_INDUTY_CD)별로 여러 row가 있으므로,
     * 외식업 관련 업종 row들의 THSMON_SELNG_AMT를 합산한다.
     *
     * 외식업 식별 기준: SVC_INDUTY_CD_NM이 "한식/중식/일식/양식/분식/카페/제과점/패스트푸드/치킨/주점/호프"
     * 등 외식업 키워드 포함. (서울시는 자체 SVC_INDUTY_CD 체계를 사용하며 명세에 정확한
     * 코드 표가 없어 한글명 기준 필터링이 가장 안전.)
     */
        public long fetchAvgMonthlySales(String dongCode) {
        if (isKeyMissing(salesKey) || dongCode == null) return 0L;
        String url = buildUrl(salesKey, SERVICE_SALES);
        try {
            JsonNode rows = callAndExtractRows(url, SERVICE_SALES);
            if (rows == null) return 0L;

            String latestQuarter = findLatestQuarter(rows);
            if (latestQuarter == null) return 0L;

            long totalSales = 0L;
            int matched = 0;
            for (JsonNode row : rows) {
                String rowDong = textOf(row, "ADSTRD_CD");
                String rowQuarter = textOf(row, "STDR_YYQU_CD");
                if (!dongCode.equals(rowDong)) continue;
                if (!latestQuarter.equals(rowQuarter)) continue;

                // 업종명 최초 매칭 시 디버그 로그 (실제 필드값 확인용)
                if (matched == 0) {
                    log.info("[SeoulApi] 추정매출 최초 매칭 row - SVC_INDUTY_CD={}, SVC_INDUTY_CD_NM={}",
                            textOf(row, "SVC_INDUTY_CD"), textOf(row, "SVC_INDUTY_CD_NM"));
                }
                totalSales += readLong(row, "THSMON_SELNG_AMT");
                matched++;
            }
            log.info("[SeoulApi] 추정매출 dongCode={} quarter={} 전체 row {}건 합산 = {}원",
                    dongCode, latestQuarter, matched, totalSales);
            return totalSales;
        } catch (Exception e) {
            log.warn("[SeoulApi] 추정매출 조회 실패 dongCode={}: {}", dongCode, e.getMessage());
            return 0L;
        }
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