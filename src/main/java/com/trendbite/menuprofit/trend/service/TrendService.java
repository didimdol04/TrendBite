package com.trendbite.menuprofit.trend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendbite.menuprofit.trend.client.FastApiTrendClient;
import com.trendbite.menuprofit.trend.dto.request.TrendRequest;
import com.trendbite.menuprofit.trend.dto.response.SimilarPattern;
import com.trendbite.menuprofit.trend.dto.response.TrendResponse;
import com.trendbite.menuprofit.trend.entity.TrendAnalysisCache;
import com.trendbite.menuprofit.trend.entity.TrendKeyword;
import com.trendbite.menuprofit.trend.entity.TrendPattern;
import com.trendbite.menuprofit.trend.repository.TrendAnalysisCacheRepository;
import com.trendbite.menuprofit.trend.repository.TrendKeywordRepository;
import com.trendbite.menuprofit.trend.repository.TrendPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendService {

    private static final TypeReference<List<SimilarPattern>> SIMILAR_PATTERNS_TYPE =
            new TypeReference<>() {};

    private final FastApiTrendClient            fastApiTrendClient;
    private final TrendAnalysisCacheRepository  cacheRepository;
    private final TrendKeywordRepository        keywordRepository;
    private final TrendPatternRepository        patternRepository;
    private final ObjectMapper                  objectMapper;

    @Value("${trend.cache.ttl-hours:6}")
    private long cacheTtlHours;

    /**
     * 트렌드 분석. 캐시 hit이면 캐시 반환, miss면 FastAPI 호출 후 저장.
     * 어느 경로든 검색 카운트(TrendKeyword)는 1 증가.
     */
    @Transactional
    public TrendResponse analyze(TrendRequest request) {
        String keyword  = request.getKeyword();
        String category = request.getCategory();
        LocalDateTime now = LocalDateTime.now();

        recordKeywordSearch(keyword, category, now);

        Optional<TrendAnalysisCache> hit =
                cacheRepository.findFirstByKeywordAndCategoryAndExpiresAtAfterOrderByCreatedAtDesc(
                        keyword, category, now);
        if (hit.isPresent()) {
            log.debug("[TrendService] cache hit keyword={}, category={}", keyword, category);
            return toResponse(hit.get());
        }

        log.debug("[TrendService] cache miss → FastAPI keyword={}, category={}", keyword, category);
        TrendResponse response = fastApiTrendClient.analyze(request);
        saveCache(keyword, category, response, now);
        return response;
    }

    public List<TrendKeyword> getPopularKeywords(int limit) {
        return keywordRepository.findAllByOrderBySearchCountDesc(PageRequest.of(0, limit));
    }

    public List<TrendPattern> getAllPatterns() {
        return patternRepository.findAll();
    }

    // ── private ────────────────────────────────────────────────

    private void recordKeywordSearch(String keyword, String category, LocalDateTime now) {
        TrendKeyword entity = keywordRepository.findByKeywordAndCategory(keyword, category)
                .orElseGet(() -> TrendKeyword.builder()
                        .keyword(keyword)
                        .category(category)
                        .searchCount(0L)
                        .firstSearchedAt(now)
                        .lastSearchedAt(now)
                        .build());
        entity.incrementSearchCount();
        keywordRepository.save(entity);
    }

    private void saveCache(String keyword, String category, TrendResponse response, LocalDateTime now) {
        String similarJson;
        try {
            List<SimilarPattern> patterns = response.getSimilarPatterns() != null
                    ? response.getSimilarPatterns()
                    : Collections.emptyList();
            similarJson = objectMapper.writeValueAsString(patterns);
        } catch (JsonProcessingException e) {
            // 캐시 저장 실패는 비즈니스 흐름을 막지 않는다 — 로그만 남기고 빈 배열.
            log.warn("[TrendService] similar_patterns 직렬화 실패: {}", e.getMessage());
            similarJson = "[]";
        }

        TrendAnalysisCache cache = TrendAnalysisCache.builder()
                .keyword(keyword)
                .category(category)
                .plcStage(response.getPlcStage())
                .overallTrend(response.getOverallTrend())
                .recentTrend(response.getRecentTrend())
                .latestInterest(response.getLatestInterest())
                .averageInterest(response.getAverageInterest())
                .trendSpeed(response.getTrendSpeed())
                .peakRatio(response.getPeakRatio())
                .growthRate(response.getGrowthRate())
                .trendScore(response.getTrendScore())
                .recommendation(response.getRecommendation())
                .similarPatternsJson(similarJson)
                .createdAt(now)
                .expiresAt(now.plusHours(cacheTtlHours))
                .build();
        cacheRepository.save(cache);
    }

    private TrendResponse toResponse(TrendAnalysisCache cache) {
        List<SimilarPattern> patterns;
        try {
            patterns = cache.getSimilarPatternsJson() == null
                    ? Collections.emptyList()
                    : objectMapper.readValue(cache.getSimilarPatternsJson(), SIMILAR_PATTERNS_TYPE);
        } catch (JsonProcessingException e) {
            // 역직렬화 실패는 캐시 데이터 손상 — 빈 리스트로 폴백.
            log.warn("[TrendService] similar_patterns 역직렬화 실패 cacheId={}, msg={}",
                    cache.getId(), e.getMessage());
            patterns = Collections.emptyList();
        }
        return TrendResponse.builder()
                .plcStage(cache.getPlcStage())
                .overallTrend(cache.getOverallTrend())
                .recentTrend(cache.getRecentTrend())
                .latestInterest(cache.getLatestInterest())
                .averageInterest(cache.getAverageInterest())
                .trendSpeed(cache.getTrendSpeed())
                .peakRatio(cache.getPeakRatio())
                .growthRate(cache.getGrowthRate())
                .trendScore(cache.getTrendScore())
                .recommendation(cache.getRecommendation())
                .similarPatterns(patterns)
                .build();
    }
}
