package com.trendbite.menuprofit.trend.repository;

import com.trendbite.menuprofit.trend.entity.TrendPattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrendPatternRepository extends JpaRepository<TrendPattern, Long> {

    boolean existsByName(String name);
}
