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
 * 소상공인시장진흥공단 상가정보 API 클라이언트.
 *
 * 행정동 코드를 key로 해당 지역 내 **경쟁 점포 수** (외식업 상호 수)를 반환한다.
 * 공공데이터 포털 API (https://www.data.go.kr):
 *   - 엔드포인트: https://apis.data.go.kr/B553077/api/open/sdsc2/storeListInDong
 *   - 핵심 파라미터: ServiceKey, divId=adongCd, key=행정동코드, indsLclsCd=외식업 대분류(I2)
 *
 * 응답은 pageNo/totalCount 구조이므로 totalCount 필드만 읽는다.
 * 실패 시 0 반환 (B-6 Fallback 정책).
 */
@Slf4j
@Component
public class SogongongApiClient {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B553077/api/open/sdsc2/storeListInDong";

    /** 업종 대분류 코드: "I2" = 음식점 (외식업) */
    private static final String INDS_LCLS_CD = "I2";

    private final RestTemplate  restTemplate;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    @Value("${sogongong.api.key:}")
    private String apiKey;

    public SogongongApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 행정동 코드로 외식업 경쟁 점포 수 조회.
     * 실패 시 0 반환.
     */
    public int fetchCompetitionCount(String dongCode) {
        if (apiKey == null || apiKey.isBlank() || dongCode == null) return 0;

        try {
            String url = String.format(
                    "%s?serviceKey=%s&type=json&divId=adongCd&key=%s&indsLclsCd=%s&numOfRows=1&pageNo=1",
                    BASE_URL,
                    URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
                    URLEncoder.encode(dongCode, StandardCharsets.UTF_8),
                    INDS_LCLS_CD
            );
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // 일반 구조: response.body.totalCount
            JsonNode totalCountNode = root.path("body").path("totalCount");
            if (!totalCountNode.isMissingNode() && !totalCountNode.isNull()) {
                return totalCountNode.asInt(0);
            }
            // 변형 구조: totalCount가 root 바로 아래인 경우
            JsonNode alt = root.path("totalCount");
            if (!alt.isMissingNode() && !alt.isNull()) {
                return alt.asInt(0);
            }
            return 0;
        } catch (Exception e) {
            log.warn("[SogongongApi] 경쟁점포 조회 실패 dongCode={}: {}", dongCode, e.getMessage());
            return 0;
        }
    }
}
