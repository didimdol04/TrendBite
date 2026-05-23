package com.trendbite.menuprofit.trend.repository;

import com.trendbite.menuprofit.trend.entity.TrendKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrendKeywordRepository extends JpaRepository<TrendKeyword, Long> {

    Optional<TrendKeyword> findByKeywordAndCategory(String keyword, String category);

    List<TrendKeyword> findAllByOrderBySearchCountDesc(Pageable pageable);

    /**
     * 부분 일치 키워드 검색 — 검색 카운트 내림차순.
     * 메뉴명 자동완성용 (GET /api/v1/trend/search-keywords?q=).
     */
    @Query("SELECT t.keyword FROM TrendKeyword t " +
            "WHERE LOWER(t.keyword) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY t.searchCount DESC")
    List<String> findKeywordsBySubstring(@Param("q") String q, Pageable pageable);
}