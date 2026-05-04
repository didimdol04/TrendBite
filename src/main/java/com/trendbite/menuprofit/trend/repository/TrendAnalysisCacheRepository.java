package com.trendbite.menuprofit.trend.repository;

import com.trendbite.menuprofit.trend.entity.TrendAnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TrendAnalysisCacheRepository extends JpaRepository<TrendAnalysisCache, Long> {

    /** 미만료 캐시 중 가장 최신 1건 조회 */
    Optional<TrendAnalysisCache>
    findFirstByKeywordAndCategoryAndExpiresAtAfterOrderByCreatedAtDesc(
            String keyword, String category, LocalDateTime now);

    /** 만료된 캐시 일괄 삭제 (배치/스케줄러용) */
    long deleteByExpiresAtBefore(LocalDateTime threshold);
}
