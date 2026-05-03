package com.trendbite.menuprofit.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 외부 API에서 수집한 4가지 raw 데이터 통합 DTO
 * LocationCalculatorService의 계산 입력값이 된다.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LocationRawData {

    /** 일 유동인구 (서울시 길단위인구-상권 API) */
    private long foottraffic;

    /** 월 추정매출 (원) (서울시 추정매출-행정동 API) */
    private long avgMonthlySales;

    /** 집객시설 수 (서울시 집객시설-상권 API) — 참고용 */
    private int facilityCount;

    /** 경쟁점포 수 (소진공 상가정보 API) */
    private int competitionCount;
}
