package com.trendbite.menuprofit.location.initializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendbite.menuprofit.location.entity.SaleStat;
import com.trendbite.menuprofit.location.repository.SaleStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 서버 기동 시 서울시 추정매출-행정동 API(VwsmAdstrdSelngW, 약 47만 건) 전체를 DB에 적재.
 *
 * 왜 DB 사전 적재인가?
 * 
 *   서울시 추정매출 API는 47만 건 bulk 구조 — 특정 행정동 코드 직접 필터 불가 (path param 미지원)
 *   1,000건 × 5페이지(= 5,000건) 방식으로는 전체의 1%만 커버 → 원하는 동 데이터를 못 찾음
 *   DB 적재 후엔 {@code SELECT ... WHERE dong_code = ?} 한 줄로 즉시 조회
 * 
 *
 * 동작 방식
 * 
 *   {@code ApplicationReadyEvent} 시점에 실행 (GeoSeedInitializer + PIP 캐시 이후)
 *   DB에 이미 데이터 있으면 skip (재기동 시 1초 이내)
 *   없으면 백그라운드 Thread로 비동기 적재 — 서버는 즉시 요청 수신 가능
 *   적재 중 사용자 요청 → {@code avgMonthlySales=0}으로 응답 (적재 완료 후부터 실데이터)
 *   475페이지 × 1,000건 = 약 47만 건, 예상 소요 4~8분
 * 
 */
@Slf4j
@Component
public class SaleStatInitializer {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";
    private static final String SERVICE  = "VwsmAdstrdSelngW";
    private static final int    PAGE_SIZE = 1000;
    private static final int    MAX_PAGES = 500;        // 1000 × 500 = 50만 건 커버
    private static final long   THROTTLE_MS = 100;      // API rate limit 방지

    private final SaleStatRepository repository;
    private final RestTemplate       restTemplate;
    private final ObjectMapper       objectMapper = new ObjectMapper();

    @Value("${seoul.api.sales-key:}")
    private String salesKey;

    public SaleStatInitializer(SaleStatRepository repository,
                               @Qualifier("restTemplate") RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long count = repository.count();
        if (count > 0) {
            log.info("[SaleStat] 이미 데이터 적재됨 (count={}), skip", count);
            return;
        }
        if (salesKey == null || salesKey.isBlank()) {
            log.warn("[SaleStat] SEOUL_SALES_KEY 미설정 — 추정매출 적재 건너뜀");
            return;
        }
        // 백그라운드 스레드로 비동기 실행
        Thread loader = new Thread(this::loadAll, "sale-stat-loader");
        loader.setDaemon(true);
        loader.start();
        log.info("[SaleStat] 추정매출 전체 적재 시작 (백그라운드 — 약 5~8분 소요)");
    }

    /**
     * 47만 건 전체 페이지네이션 → 행정동별 최신 분기 외식업 매출 합산 → DB 저장.
     */
    private void loadAll() {
        long startTime = System.currentTimeMillis();

        // dongCode → quarter → salesSum
        Map<String, Map<String, Long>> byDongQuarter = new HashMap<>();
        // dongCode → dongName (참조용)
        Map<String, String> dongNames = new HashMap<>();

        int totalRows = 0;
        int totalPages = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            int start = (page - 1) * PAGE_SIZE + 1;
            int end   = page * PAGE_SIZE;
            String url = String.format("%s/%s/json/%s/%d/%d", BASE_URL, salesKey, SERVICE, start, end);

            try {
                String response = restTemplate.getForObject(url, String.class);
                if (response == null) break;

                JsonNode root = objectMapper.readTree(response);
                JsonNode serviceNode = root.get(SERVICE);
                if (serviceNode == null) break;

                // RESULT 코드 확인
                JsonNode result = serviceNode.get("RESULT");
                if (result != null) {
                    String code = result.has("CODE") ? result.get("CODE").asText() : "";
                    if (!"INFO-000".equals(code)) {
                        log.warn("[SaleStat] API 에러 page {}: {} - {}", page,
                                code, result.has("MESSAGE") ? result.get("MESSAGE").asText() : "");
                        break;
                    }
                }

                JsonNode rows = serviceNode.get("row");
                if (rows == null || !rows.isArray() || rows.isEmpty()) break;

                for (JsonNode row : rows) {
                    String dongCode   = textOf(row, "ADSTRD_CD");
                    String dongName   = textOf(row, "ADSTRD_CD_NM");
                    String quarter    = textOf(row, "STDR_YYQU_CD");
                    String indutyName = textOf(row, "SVC_INDUTY_CD_NM");

                    if (dongCode == null || quarter == null) continue;
                    // 외식업 필터
                    if (!isFoodIndustry(indutyName)) continue;

                    long amount = readLong(row, "THSMON_SELNG_AMT");
                    byDongQuarter
                            .computeIfAbsent(dongCode, k -> new HashMap<>())
                            .merge(quarter, amount, Long::sum);
                    if (dongName != null) dongNames.putIfAbsent(dongCode, dongName);
                }

                totalRows += rows.size();
                totalPages = page;

                // 마지막 페이지 도달
                if (rows.size() < PAGE_SIZE) break;

                // 진행 로그 (50페이지마다)
                if (page % 50 == 0) {
                    log.info("[SaleStat] 적재 진행 중 — {}페이지 ({}건), {}개 행정동 수집",
                            page, totalRows, byDongQuarter.size());
                }

                Thread.sleep(THROTTLE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[SaleStat] 적재 중단됨 (InterruptedException)");
                break;
            } catch (Exception e) {
                log.warn("[SaleStat] 페이지 {} 실패: {} — 계속 진행", page, e.getMessage());
            }
        }

        // 각 행정동의 최신 분기만 추출하여 DB 저장
        LocalDateTime now = LocalDateTime.now();
        List<SaleStat> stats = byDongQuarter.entrySet().stream()
                .map(entry -> {
                    String dongCode = entry.getKey();
                    Map<String, Long> quarterMap = entry.getValue();
                    String latestQ = quarterMap.keySet().stream()
                            .max(String::compareTo).orElse(null);
                    return SaleStat.builder()
                            .dongCode(dongCode)
                            .dongName(dongNames.get(dongCode))
                            .latestQuarter(latestQ)
                            .foodMonthlySales(latestQ != null ? quarterMap.get(latestQ) : 0L)
                            .loadedAt(now)
                            .build();
                })
                .collect(Collectors.toList());

        if (!stats.isEmpty()) {
            repository.saveAll(stats);
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        log.info("[SaleStat] 적재 완료 — {}개 행정동, {}페이지({}건) 처리, {}초 소요",
                stats.size(), totalPages, totalRows, elapsed);
    }

    // ──────────────────────────────── helpers ────────────────────────────────

    private boolean isFoodIndustry(String indutyName) {
        if (indutyName == null) return false;
        return indutyName.contains("한식") || indutyName.contains("중식")
                || indutyName.contains("일식") || indutyName.contains("양식")
                || indutyName.contains("분식") || indutyName.contains("카페")
                || indutyName.contains("제과") || indutyName.contains("패스트푸드")
                || indutyName.contains("치킨") || indutyName.contains("호프")
                || indutyName.contains("주점") || indutyName.contains("커피")
                || indutyName.contains("음식") || indutyName.contains("베이커리")
                || indutyName.contains("아이스크림") || indutyName.contains("샤브")
                || indutyName.contains("피자") || indutyName.contains("뷔페");
    }

    private String textOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private long readLong(JsonNode node, String key) {
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) return 0L;
        try { return (long) v.asDouble(); }
        catch (Exception ignored) { return 0L; }
    }
}
