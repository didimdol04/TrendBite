package com.trendbite.menuprofit.location.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
 *     - divId        : "adongCd" (행정동 검색)
 *     - key          : 행정동 코드 (8자리, 예: 11080600)
 *     - indsLclsCd   : "I2" (음식점업 대분류)
 *     - numOfRows    : 1 (totalCount만 필요해서 최소)
 *     - pageNo       : 1
 *     - type         : "json"
 *
 * 응답 구조:
 *   { header: { resultCode: "00", ...},
 *     body:   { totalCount: <int>, items: { item: [...] }, ... } }
 *
 * 우리는 totalCount(해당 행정동 외식업 점포 총수)만 사용한다.
 * 실패 시 0 반환 (B-6 Fallback 정책).
 */
@Slf4j
@Component
public class SogongongApiClient {

    private static final String BASE_URL =
            "http://apis.data.go.kr/B553077/api/open/sdsc2/storeListInDong";

    /** 외식업 대분류 코드: "I2" = 음식점업 (업종분류 v1 확인) */
    private static final String INDS_LCLS_CD = "I2";

    private final RestTemplate  restTemplate;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    @Value("${sogongong.api.key:}")
    private String apiKey;

    public SogongongApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 행정동 코드로 외식업(I2) 경쟁 점포 수 조회.
     * 응답의 body.totalCount 값 반환.
     */
public int fetchCompetitionCount(String dongCode) {
    if (apiKey == null || apiKey.isBlank() || dongCode == null) return 0;

    // 1차 시도: 행정동 코드(adongCd)로 조회
    int result = callSogongongApi("adongCd", dongCode);
    if (result > 0) return result;

    // 2차 시도: 행정동 코드 없음 → 시군구(signguCd) 단위로 fallback
    // 행정동 코드 앞 5자리 = 시군구 코드 (예: 11080600 → 11080)
    if (dongCode.length() >= 5) {
        String signguCd = dongCode.substring(0, 5);
        log.info("[SogongongApi] adongCd 조회 없음, signguCd={} fallback 시도", signguCd);
        result = callSogongongApi("signguCd", signguCd);
    }
    return result;
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
            if (response == null) return 0;

            JsonNode root = objectMapper.readTree(response);
            // 공공데이터포털 표준 응답: {"response":{"header":...,"body":...}}
            JsonNode container = root.has("response") ? root.get("response") : root;

            String resultCode = container.path("header").path("resultCode").asText("");
            if (!"00".equals(resultCode)) {
                log.debug("[SogongongApi] {} divId={} key={}: {} - {}",
                        resultCode.equals("03") ? "NODATA" : "에러",
                        divId, key, resultCode,
                        container.path("header").path("resultMsg").asText());
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