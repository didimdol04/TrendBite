package com.trendbite.menuprofit.trend.repository;

import com.trendbite.menuprofit.trend.entity.TrendKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrendKeywordRepository extends JpaRepository<TrendKeyword, Long> {

    Optional<TrendKeyword> findByKeywordAndCategory(String keyword, String category);

    List<TrendKeyword> findAllByOrderBySearchCountDesc(Pageable pageable);
}
