package com.trendbite.menuprofit.location.service;

import org.springframework.stereotype.Service;

/**
 * 입지 보정계수 산출 핵심 서비스.
 *
 * 공식 (계획서 v2.0 5-1절):
 *   t = normalize(traffic,     5_000 .. 40_000)
 *   s = normalize(sales,  10_000_000 .. 50_000_000)
 *   c = normalize(comp,         0 ..     30)
 *   raw = 0.4 * t + 0.4 * s - 0.2 * c          (이론 범위 -0.2 ~ 0.8)
 *   multiplier = 0.8 + 0.4 * ((raw + 0.2) / 1.0)  (이론 범위 0.8 ~ 1.2)
 *
 * Min-Max 기준값은 초기 추정치이며, 실데이터 수집 후 상수 조정 가능하다.
 */
@Service
public class LocationCalculatorService {

    // Min-Max 기준값 (v2.0 명세서)
    public static final double TRAFFIC_MIN =       5_000.0;
    public static final double TRAFFIC_MAX =      40_000.0;
    public static final double SALES_MIN   =  10_000_000.0;
    public static final double SALES_MAX   =  50_000_000.0;
    public static final int    COMP_MIN    =            0;
    public static final int    COMP_MAX    =           30;

    // 가중치
    public static final double WEIGHT_TRAFFIC     =  0.40;
    public static final double WEIGHT_SALES       =  0.40;
    public static final double WEIGHT_COMPETITION = -0.20;

    // 최종 범위 클램핑
    public static final double MULTIPLIER_MIN = 0.80;
    public static final double MULTIPLIER_MAX = 1.20;

    /**
     * 값을 [0, 1]로 Min-Max 정규화.
     * max == min인 특수 케이스는 0.5를 반환 (중립값).
     */
    public double normalize(double value, double min, double max) {
        if (max == min) return 0.5;
        double ratio = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    /**
     * 입지 보정계수 계산 (소수 3자리로 반올림, 0.8~1.2로 클램핑).
     */
    public double calculate(long traffic, long sales, int competition) {
        double t = normalize(traffic,     TRAFFIC_MIN, TRAFFIC_MAX);
        double s = normalize(sales,       SALES_MIN,   SALES_MAX);
        double c = normalize(competition, COMP_MIN,    COMP_MAX);

        double raw = (WEIGHT_TRAFFIC * t) + (WEIGHT_SALES * s) + (WEIGHT_COMPETITION * c);
        double multiplier = 0.8 + 0.4 * ((raw + 0.2) / 1.0);
        double clamped = Math.max(MULTIPLIER_MIN, Math.min(MULTIPLIER_MAX, multiplier));
        return Math.round(clamped * 1000.0) / 1000.0;
    }

    /**
     * raw_score 자체를 반환 (Response DTO에 표시용, 소수 3자리 반올림).
     */
    public double calculateRawScore(double trafficScore, double salesScore, double competitionScore) {
        double raw = (WEIGHT_TRAFFIC * trafficScore)
                   + (WEIGHT_SALES   * salesScore)
                   + (WEIGHT_COMPETITION * competitionScore);
        return Math.round(raw * 1000.0) / 1000.0;
    }

    /**
     * multiplier → 입지 등급 (1~10).
     * 1.20 → 등급 1 (최우수), 0.80 → 등급 10 (최하).
     */
    public int calculateGrade(double multiplier) {
        int grade = (int) Math.round(10 - ((multiplier - MULTIPLIER_MIN) / (MULTIPLIER_MAX - MULTIPLIER_MIN)) * 9.0);
        return Math.max(1, Math.min(10, grade));
    }
}
