package com.trendbite.menuprofit.location.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 행정동별 외식업 추정매출 합산 데이터.
 *
 * 서울시 열린데이터광장 추정매출-행정동 API(VwsmAdstrdSelngW)가 47만 건 bulk 구조로
 * 실시간 필터 조회가 불가능한 문제를 해결하기 위해, 서버 최초 기동 시 전체 데이터를
 * 적재하여 DB 조회 방식으로 전환.
 *
 * PK는 ADSTRD_CD(행정동 코드, 서울시 8자리 체계).
 * 값은 해당 동의 최신 분기 외식업 업종 THSMON_SELNG_AMT 합산.
 *
 * @see com.trendbite.menuprofit.location.initializer.SaleStatInitializer
 */
@Entity
@Table(name = "sale_stat")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleStat {

    /** 행정동 코드 (ADSTRD_CD, 서울시 8자리 — 예: 11080600) */
    @Id
    @Column(name = "dong_code", length = 20)
    private String dongCode;

    /** 행정동명 (ADSTRD_CD_NM — 예: 안암동) */
    @Column(name = "dong_name", length = 100)
    private String dongName;

    /** 데이터 기준 분기 (STDR_YYQU_CD — 예: 20232 = 2023년 2분기) */
    @Column(name = "latest_quarter", length = 10)
    private String latestQuarter;

    /** 외식업 업종 월 추정매출 합산 (원) */
    @Column(name = "food_monthly_sales")
    private Long foodMonthlySales;

    /** 적재 시각 */
    @Column(name = "loaded_at")
    private LocalDateTime loadedAt;
}
