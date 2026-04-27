package com.trendbite.menuprofit.trend.client;

import com.trendbite.menuprofit.trend.dto.request.ReportRequest;
import com.trendbite.menuprofit.trend.dto.response.ReportResponse;
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
public class FastApiReportClient {

    private static final String PATH = "/api/v1/report/generate";

    private final RestTemplate restTemplate;
    private final String       baseUrl;

    public FastApiReportClient(
            @Qualifier("fastApiRestTemplate") RestTemplate restTemplate,
            @Value("${fastapi.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
    }

    public ReportResponse generate(ReportRequest request) {
        String url = baseUrl + PATH;
        try {
            ResponseEntity<ReportResponse> res =
                    restTemplate.postForEntity(url, request, ReportResponse.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                throw new FastApiCallException(
                        "FastAPI report/generate 응답 비정상: status=" + res.getStatusCode());
            }
            return res.getBody();
        } catch (RestClientException e) {
            log.error("[FastApiReportClient] 호출 실패: keyword={}, msg={}",
                    request.getKeyword(), e.getMessage());
            throw new FastApiCallException("FastAPI report/generate 호출 실패", e);
        }
    }
}
