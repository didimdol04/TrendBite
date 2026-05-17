package com.trendbite.menuprofit.location.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 소상공인시장진흥공단 상가정보 API 클라이언트 (행정동 단위 상가업소 조회).
 *
 * 명세서 (2025.6 활용가이드 기준):
 *   엔드포인트: http://apis.data.go.kr/B553077/api/open/sdsc2/storeListInDong
 *   파라미터:
 *     - serviceKey   : 인증키 (URL Encoded)
 *     - divId        : "adongCd" (행정동 검색) / "signguCd" (시군구 fallback)
 *     - key          : 행정동 코드 (8자리, 예: 11080600) / 시군구 코드 (5자리)
 *     - indsLclsCd   : "I2" (음식점업 대분류)
 *     - numOfRows    : 1 (totalCount만 필요해서 최소)
 *     - pageNo       : 1
 *     - type         : "json"
 *
 * <h3>v3 변경 사항 (round 2)</h3>
 * <ul>
 *   <li>(P0-3) resultCode별 분기 처리 강화 - 정상(00) / NODATA(03) / 시스템 에러(99) 등 명시적 분류</li>
 *   <li>(P0-3) raw response 일부를 DEBUG로 출력 (실제 응답 구조 확인용)</li>
 *   <li>(P0-3) signguCd fallback 결과 명시적 INFO 로그 (이전에는 silent fail이라 디버깅 어려움)</li>
 *   <li>(P4-2) 생성자에 {@code @Qualifier("restTemplate")} 명시</li>
 * </ul>
 */
@Slf4j
@Component
public class SogongongApiClient {

    private static final String BASE_URL =
            "http://apis.data.go.kr/B553077/api/open/sdsc2/storeListInDong";

    /** 외식업 대분류 코드: "I2" = 음식점업 (업종분류 v1 확인) */
    private static final String INDS_LCLS_CD = "I2";

    // (P0-3) 공공데이터포털 표준 resultCode
    private static final String CODE_OK     = "00";
    private static final String CODE_NODATA = "03";

    private final RestTemplate  restTemplate;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    @Value("${sogongong.api.key:}")
    private String apiKey;

    /**
     * (P4-2) RestTemplate Bean 모호성 제거 — location 도메인의 RestTemplate(timeout 5/10s)를
     * 정확히 지정해서 트렌드 도메인의 fastApiRestTemplate가 잘못 주입되지 않게 한다.
     */
    public SogongongApiClient(@Qualifier("restTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 행정동 코드로 외식업(I2) 경쟁 점포 수 조회.
     * 응답의 body.totalCount 값 반환.
     * <p>
     * 1차: adongCd로 행정동 단위 조회
     * 2차: 결과 없거나 0이면 signguCd로 시군구 단위 fallback (행정동 코드 앞 5자리)
     */
    public int fetchCompetitionCount(String dongCode) {
        if (apiKey == null || apiKey.isBlank() || dongCode == null) return 0;

        // 1차 시도: 행정동 코드(adongCd)
        int result = callSogongongApi("adongCd", dongCode);
        if (result > 0) return result;

        // 2차 시도: 시군구(signguCd) fallback — 행정동 코드 앞 5자리
        if (dongCode.length() >= 5) {
            String signguCd = dongCode.substring(0, 5);
            log.info("[SogongongApi] adongCd={} 결과 없음 → signguCd={} fallback 시도",
                    dongCode, signguCd);
            int signguResult = callSogongongApi("signguCd", signguCd);
            // (P0-3) fallback 결과 명시적 로그
            log.info("[SogongongApi] signguCd={} fallback 결과 = {}건", signguCd, signguResult);
            return signguResult;
        }
        return 0;
    }

    private int callSogongongApi(String divId, String key) {
        try {
            String url = String.format(
                    "%s?serviceKey=%s&type=json&divId=%s&key=%s&indsLclsCd=%s&numOfRows=1&pageNo=1",
                    BASE_URL,
                    URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
                    divId,
                    URLEncoder.encode(key, StandardCharsets.UTF_8),
                    INDS_LCLS_CD
            );

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("[SogongongApi] divId={} key={} 응답 null", divId, key);
                return 0;
            }

            // (P0-3) raw response 일부 디버그 로그
            if (log.isDebugEnabled()) {
                String preview = response.length() > 500
                        ? response.substring(0, 500) + "..." : response;
                log.debug("[SogongongApi] divId={} key={} raw response: {}", divId, key, preview);
            }

            JsonNode root = objectMapper.readTree(response);
            // 공공데이터포털 표준 응답: {"response":{"header":...,"body":...}}
            JsonNode container = root.has("response") ? root.get("response") : root;

            String resultCode = container.path("header").path("resultCode").asText("");
            String resultMsg  = container.path("header").path("resultMsg").asText("");

            // (P0-3) resultCode별 명시적 분기
            if (CODE_NODATA.equals(resultCode)) {
                log.info("[SogongongApi] NODATA divId={} key={} (정상 응답이지만 데이터 없음)",
                        divId, key);
                return 0;
            }
            if (!CODE_OK.equals(resultCode)) {
                log.warn("[SogongongApi] API 에러 divId={} key={}: code={} msg={}",
                        divId, key, resultCode, resultMsg);
                return 0;
            }

            int count = container.path("body").path("totalCount").asInt(0);
            log.info("[SogongongApi] divId={} key={} 외식업 점포 수={}", divId, key, count);
            return count;
        } catch (Exception e) {
            log.warn("[SogongongApi] 조회 실패 divId={} key={}: {}", divId, key, e.getMessage());
            return 0;
        }
    }
}
