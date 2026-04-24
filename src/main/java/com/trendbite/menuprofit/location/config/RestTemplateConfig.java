package com.trendbite.menuprofit.location.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 서울시/소진공 API 호출용 RestTemplate 빈 등록.
 *
 * 이미 RestTemplate Bean이 존재한다면 본 Config 파일은 삭제할 것.
 * (BeanDefinitionOverride 에러 방지)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
