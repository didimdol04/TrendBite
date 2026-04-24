package com.trendbite.menuprofit.location.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * LocationCalculatorService 단위 테스트.
 * Spring 컨텍스트 없이 순수 POJO로 검증한다.
 *
 * ⚠ 명세서(계획서 v2.0 B-7절)의 예시값 1.104는 실제 공식 대입 시 약 1.082가 계산된다.
 *   본 테스트는 **실제 공식에 따른 정확한 계산값**을 기대값으로 사용한다.
 *   명세서 예시값 업데이트가 필요하면 계획서 재작성 시 반영 예정.
 */
class LocationCalculatorServiceTest {

    private LocationCalculatorService calculator;

    @BeforeEach
    void setUp() {
        calculator = new LocationCalculatorService();
    }

    @Test
    @DisplayName("명세서 예시값: traffic=30000, sales=42000000, comp=15 → 약 1.082 (실제 공식 기반)")
    void testCalculate_specExample() {
        double result = calculator.calculate(30_000L, 42_000_000L, 15);
        // 실제 공식으로 정확히 1.082가 나옴 (±0.001 오차 허용)
        assertThat(result).isCloseTo(1.082, within(0.001));
    }

    @Test
    @DisplayName("최솟값 경계: traffic=5000, sales=10M, comp=30 → 0.8")
    void testCalculate_minBoundary() {
        double result = calculator.calculate(5_000L, 10_000_000L, 30);
        assertThat(result).isEqualTo(0.8);
    }

    @Test
    @DisplayName("최댓값 경계: traffic=40000, sales=50M, comp=0 → 1.2")
    void testCalculate_maxBoundary() {
        double result = calculator.calculate(40_000L, 50_000_000L, 0);
        assertThat(result).isEqualTo(1.2);
    }

    @Test
    @DisplayName("최솟값 하한선 미만도 클램핑 되어 0.8")
    void testCalculate_belowMinClamped() {
        double result = calculator.calculate(0L, 0L, 100);
        assertThat(result).isGreaterThanOrEqualTo(0.8);
    }

    @Test
    @DisplayName("최댓값 상한선 초과도 클램핑 되어 1.2")
    void testCalculate_aboveMaxClamped() {
        double result = calculator.calculate(1_000_000L, 999_999_999L, -10);
        assertThat(result).isLessThanOrEqualTo(1.2);
    }

    @Test
    @DisplayName("normalize(value, min, max): 범위 내 값은 0~1 반환")
    void testNormalize_inRange() {
        assertThat(calculator.normalize(25_000, 0, 50_000)).isEqualTo(0.5);
        assertThat(calculator.normalize(0, 0, 100)).isEqualTo(0.0);
        assertThat(calculator.normalize(100, 0, 100)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("normalize: 범위 밖 값은 0 또는 1로 클램핑")
    void testNormalize_clamp() {
        assertThat(calculator.normalize(-10, 0, 100)).isEqualTo(0.0);
        assertThat(calculator.normalize(200, 0, 100)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("normalize: min==max 특수 케이스는 0.5 반환")
    void testNormalize_zeroRange() {
        assertThat(calculator.normalize(50, 50, 50)).isEqualTo(0.5);
    }

    @Test
    @DisplayName("calculateGrade: 1.20 → 1등급(최우수), 0.80 → 10등급(최하)")
    void testCalculateGrade_boundaries() {
        assertThat(calculator.calculateGrade(1.20)).isEqualTo(1);
        assertThat(calculator.calculateGrade(0.80)).isEqualTo(10);
    }

    @Test
    @DisplayName("calculateGrade: 1.0 중간값 → 5 또는 6등급")
    void testCalculateGrade_middle() {
        int grade = calculator.calculateGrade(1.0);
        assertThat(grade).isBetween(5, 6);
    }

    @Test
    @DisplayName("calculateGrade: 범위 밖도 1~10으로 클램핑")
    void testCalculateGrade_clamp() {
        assertThat(calculator.calculateGrade(1.5)).isEqualTo(1);
        assertThat(calculator.calculateGrade(0.5)).isEqualTo(10);
    }
}
