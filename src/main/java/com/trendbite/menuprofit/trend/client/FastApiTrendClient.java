package com.trendbite.menuprofit.trend.client;

import com.trendbite.menuprofit.trend.dto.request.TrendRequest;
import com.trendbite.menuprofit.trend.dto.response.TrendResponse;
import com.trendbite.menuprofit.trend.exception.FastApiCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class FastApiTrendClient {

    private static final String PATH = "/api/v1/trend/analyze";

    private final RestTemplate restTemplate;
    private final String       baseUrl;

    public FastApiTrendClient(
            @Qualifier("fastApiRestTemplate") RestTemplate restTemplate,
            @Value("${fastapi.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
    }

    public TrendResponse analyze(TrendRequest request) {
        String url = baseUrl + PATH;
        try {
            ResponseEntity<TrendResponse> res =
                    restTemplate.postForEntity(url, request, TrendResponse.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                throw new FastApiCallException(
                        "FastAPI trend/analyze 응답 비정상: status=" + res.getStatusCode());
            }
            return res.getBody();
        } catch (RestClientException e) {
            log.error("[FastApiTrendClient] 호출 실패: keyword={}, category={}, msg={}",
                    request.getKeyword(), request.getCategory(), e.getMessage());
            throw new FastApiCallException("FastAPI trend/analyze 호출 실패", e);
        }
    }
}
