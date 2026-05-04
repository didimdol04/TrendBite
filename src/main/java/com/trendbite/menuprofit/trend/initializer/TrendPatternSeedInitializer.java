package com.trendbite.menuprofit.trend.initializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendbite.menuprofit.trend.entity.TrendPattern;
import com.trendbite.menuprofit.trend.repository.TrendPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 부팅 시 trend_pattern 테이블에 DTW 비교 기준 시드 패턴을 적재한다.
 *
 * 시드 정의는 FastAPI trend.py 의 PAST_PATTERNS와 동일하다.
 * 운영자가 추가한 패턴(seedData=false)과 구분하기 위해 seedData=true로 표시한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendPatternSeedInitializer implements CommandLineRunner {

    private final TrendPatternRepository patternRepository;
    private final ObjectMapper objectMapper;

    private record SeedPattern(String name, String lifecycle, List<Integer> series) {}

    private static final List<SeedPattern> SEEDS = List.of(
            new SeedPattern("탕후루",      "단기급락",
                    List.of(5, 10, 40, 100, 80, 40, 20, 10, 5, 2, 1, 1)),
            new SeedPattern("흑당버블티",   "유행후정체",
                    List.of(10, 30, 80, 100, 90, 70, 50, 40, 30, 25, 20, 20)),
            new SeedPattern("마라탕",      "지속성장",
                    List.of(10, 15, 25, 40, 55, 70, 85, 95, 100, 98, 97, 99))
    );

    @Override
    @Transactional
    public void run(String... args) {
        for (SeedPattern seed : SEEDS) {
            if (patternRepository.existsByName(seed.name())) {
                log.info("[TrendPattern Seed] '{}' 이미 존재 — skip", seed.name());
                continue;
            }
            try {
                String json = objectMapper.writeValueAsString(seed.series());
                patternRepository.save(TrendPattern.builder()
                        .name(seed.name())
                        .lifecycle(seed.lifecycle())
                        .seriesJson(json)
                        .seriesLength(seed.series().size())
                        .seedData(true)
                        .build());
                log.info("[TrendPattern Seed] '{}' 적재 완료", seed.name());
            } catch (JsonProcessingException e) {
                // 시드 직렬화 실패는 코드 버그 — 다른 시드는 계속 시도하되 명확히 에러 로그.
                log.error("[TrendPattern Seed] '{}' 직렬화 실패: {}", seed.name(), e.getMessage(), e);
            }
        }
    }
}
