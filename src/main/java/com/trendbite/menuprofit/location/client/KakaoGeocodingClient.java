package com.trendbite.menuprofit.location.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * (P5-2) Kakao Geocoding API 클라이언트 — 백엔드 옵션 (스켈레톤).
 *
 * <h3>현재 사용 안 함</h3>
 * 프론트엔드가 Kakao Postcode/Geocoder로 좌표를 받아 백엔드에 전송하는 방식이 기본 흐름.
 * 본 클라이언트는 보안 + 일관성을 위해 백엔드에서 직접 변환하는 옵션으로 제공.
 *
 * <h3>활성화 조건</h3>
 * <ul>
 *   <li>{@code kakao.api.rest-key} 환경변수 설정 (Kakao Developers 콘솔에서 발급)</li>
 *   <li>{@link com.trendbite.menuprofit.location.controller.LocationController}에서
 *       address-only 요청을 받을 때 호출하도록 변경</li>
 * </ul>
 *
 * <h3>장단점</h3>
 * <ul>
 *   <li>👍 프론트의 Kakao 키 노출 없음 + 백엔드 단일 책임</li>
 *   <li>👎 백엔드 1회 추가 HTTP 호출 → 응답 지연 (~200ms)</li>
 *   <li>👎 Kakao API 일일 할당량 백엔드 단일 키로 통합 — 트래픽 증가 시 분산 어려움</li>
 * </ul>
 *
 * <p>도입 전 김수현/황예원과 합의 필요.
 */
@Slf4j
@Component
public class KakaoGeocodingClient {

    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/address.json";

    private final RestTemplate  restTemplate;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    @Value("${kakao.api.rest-key:}")
    private String restApiKey;

    public KakaoGeocodingClient(@Qualifier("restTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 주소 → (lat, lng) WGS84 좌표 변환.
     * 키 미설정·결과 없음·실패 시 {@link Optional#empty()}.
     */
    public Optional<GeocodingResult> geocode(String address) {
        if (restApiKey == null || restApiKey.isBlank() || address == null || address.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("query", address)
                    .build()
                    .encode()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + restApiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[KakaoGeocode] 응답 비정상 address={} status={}", address, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode documents = root.get("documents");
            if (documents == null || documents.size() == 0) {
                log.info("[KakaoGeocode] 결과 없음 address={}", address);
                return Optional.empty();
            }

            JsonNode first = documents.get(0);
            // Kakao 응답: x=경도, y=위도
            double lng = Double.parseDouble(first.get("x").asText());
            double lat = Double.parseDouble(first.get("y").asText());
            String normalized = first.has("address_name")
                    ? first.get("address_name").asText() : address;
            return Optional.of(new GeocodingResult(lat, lng, normalized));
        } catch (Exception e) {
            log.warn("[KakaoGeocode] 변환 실패 address={}: {}", address, e.getMessage());
            return Optional.empty();
        }
    }

    /** 지오코딩 결과 DTO (record) */
    public record GeocodingResult(double lat, double lng, String normalizedAddress) {}
}
