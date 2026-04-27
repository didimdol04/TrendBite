package com.trendbite.menuprofit.trend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * FastAPI 트렌드/리포트 서버 호출 전용 RestTemplate.
 *
 * 협업자의 기본 {@code restTemplate}(5/10s)와 분리해서 별도 빈으로 등록한다.
 * 모든 요청에 자동으로 {@code X-API-Key} 헤더를 부착한다.
 */
@Configuration
public class FastApiClientConfig {

    @Bean
    public RestTemplate fastApiRestTemplate(
            RestTemplateBuilder builder,
            @Value("${fastapi.api-key}") String apiKey,
            @Value("${fastapi.timeout-seconds:30}") int timeoutSeconds
    ) {
        ClientHttpRequestInterceptor apiKeyInterceptor = (request, body, execution) -> {
            request.getHeaders().add("X-API-Key", apiKey);
            return execution.execute(request, body);
        };

        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .additionalInterceptors(apiKeyInterceptor)
                .build();
    }
}
