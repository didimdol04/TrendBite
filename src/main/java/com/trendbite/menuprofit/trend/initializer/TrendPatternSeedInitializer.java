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

    /**
     * FastAPI trend.py PAST_PATTERNS 와 동기화 (2026-05 기준).
     * 14개월 정점 ±7 윈도우, 메뉴별 max=100 으로 재정규화한 실 네이버 데이터랩 값.
     * Spring 측에서는 series 가 외부 노출 안 되므로 정수 반올림 사용.
     */
    private static final List<SeedPattern> SEEDS = List.of(
            new SeedPattern("탕후루",       "단기급락",
                    List.of(11, 14, 16, 31, 44, 71, 100, 97, 60, 34, 29, 26, 24, 15)),
            new SeedPattern("두바이초콜릿", "단기급락",
                    List.of(0,  0,  0,  0,  15, 51, 100, 54, 27, 13, 8,  6,  5,  6)),
            new SeedPattern("달고나커피",   "유행후정체",
                    List.of(1,  3,  100, 53, 12, 4,  2,   1,  1,  1,  1,  1,  1,  1)),
            new SeedPattern("붕어빵",       "계절성",
                    List.of(9,  8,  11, 15, 54, 88, 100, 55, 26, 15, 11, 9,  7,  8)),
            new SeedPattern("키토김밥",     "안정정착",
                    List.of(43, 43, 43, 46, 42, 55, 100, 31, 24, 28, 26, 30, 32, 36)),
            new SeedPattern("마라탕",       "지속성장",
                    List.of(67, 74, 82, 88, 90, 95, 100, 83, 82, 93, 78, 82, 85, 88)),
            new SeedPattern("약과",         "재유행",
                    List.of(29, 49, 48, 32, 38, 44, 100, 60, 64, 69, 54, 39, 30, 33))
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
