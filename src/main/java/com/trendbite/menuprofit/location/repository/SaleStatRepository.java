package com.trendbite.menuprofit.location.repository;

import com.trendbite.menuprofit.location.entity.SaleStat;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 행정동별 외식업 추정매출 DB 조회.
 *
 * PK가 dongCode(ADSTRD_CD)이므로 {@code findById(dongCode)}로 직접 조회.
 * LocationAnalysisService에서 SeoulApiClient.fetchAvgMonthlySales() 대신 사용.
 */
public interface SaleStatRepository extends JpaRepository<SaleStat, String> {
    // findById(dongCode) 사용 — PK가 String dongCode
}
