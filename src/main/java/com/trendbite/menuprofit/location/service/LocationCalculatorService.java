package com.trendbite.menuprofit.location.service;

import com.trendbite.menuprofit.location.config.LocationProperties;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <h3>v3 변경 사항 (round 2)</h3>
 * <ul>
 *   <li>(P2-1) Min-Max 기준값을 {@link LocationProperties}로 외부화.
 *              기존 {@code static final TRAFFIC_MIN/MAX} 등은 {@code @Deprecated} 상태로 호환성 유지.</li>
 *   <li>(P2-1) {@link #normalizeTraffic(long)}, {@link #normalizeSales(long)},
 *              {@link #normalizeCompetition(int)} 도메인 편의 메서드 추가 (인스턴스 필드 사용).</li>
 *   <li>가중치(WEIGHT_*)와 결과 범위(MULTIPLIER_MIN/MAX)는 계산 공식 자체이므로 상수 유지.</li>
 * </ul>
 *
 * <h3>v2 (P4-1, 이전 라운드)</h3>
 * {@link #calculateFromScores(double, double, double)}로 normalize 중복 호출 제거.
 */
@Service
public class LocationCalculatorService {

    // ──────────────────────────────── 기본값 상수 (호환성) ────────────────────────────────
    public static final double DEFAULT_TRAFFIC_MIN =       5_000.0;
    public static final double DEFAULT_TRAFFIC_MAX =      40_000.0;
    public static final double DEFAULT_SALES_MIN   =  10_000_000.0;
    public static final double DEFAULT_SALES_MAX   =  50_000_000.0;
    public static final int    DEFAULT_COMP_MIN    =            0;
    public static final int    DEFAULT_COMP_MAX    =           30;

    /** @deprecated (P2-1) {@link LocationProperties} 또는 {@link #DEFAULT_TRAFFIC_MIN} 사용. */
    @Deprecated public static final double TRAFFIC_MIN = DEFAULT_TRAFFIC_MIN;
    /** @deprecated (P2-1) */ @Deprecated public static final double TRAFFIC_MAX = DEFAULT_TRAFFIC_MAX;
    /** @deprecated (P2-1) */ @Deprecated public static final double SALES_MIN   = DEFAULT_SALES_MIN;
    /** @deprecated (P2-1) */ @Deprecated public static final double SALES_MAX   = DEFAULT_SALES_MAX;
    /** @deprecated (P2-1) */ @Deprecated public static final int    COMP_MIN    = DEFAULT_COMP_MIN;
    /** @deprecated (P2-1) */ @Deprecated public static final int    COMP_MAX    = DEFAULT_COMP_MAX;

    // 가중치 (계산 공식 자체) — 상수 유지
    public static final double WEIGHT_TRAFFIC     =  0.40;
    public static final double WEIGHT_SALES       =  0.40;
    public static final double WEIGHT_COMPETITION = -0.20;

    // 최종 범위 클램핑 — 상수 유지
    public static final double MULTIPLIER_MIN = 0.80;
    public static final double MULTIPLIER_MAX = 1.20;

    // ──────────────────────────────── 인스턴스 필드 (P2-1) ────────────────────────────────
    private final double trafficMin, trafficMax;
    private final double salesMin,   salesMax;
    private final int    compMin,    compMax;

    /**
     * (P2-1) Spring DI 생성자 — {@link LocationProperties}에서 Min-Max 기준값 주입.
     * <p>application.properties에 키가 없으면 LocationProperties 필드 초기화의 기본값(명세 v2.0 추정치)이 사용된다.
     */
    @Autowired
    public LocationCalculatorService(LocationProperties props) {
        this.trafficMin = props.getTrafficMin();
        this.trafficMax = props.getTrafficMax();
        this.salesMin   = props.getSalesMin();
        this.salesMax   = props.getSalesMax();
        this.compMin    = props.getCompMin();
        this.compMax    = props.getCompMax();
    }

    /**
     * 단위 테스트용 기본값 생성자 — LocationProperties 없이 명세 v2.0 추정치 사용.
     */
    public LocationCalculatorService() {
        this.trafficMin = DEFAULT_TRAFFIC_MIN;
        this.trafficMax = DEFAULT_TRAFFIC_MAX;
        this.salesMin   = DEFAULT_SALES_MIN;
        this.salesMax   = DEFAULT_SALES_MAX;
        this.compMin    = DEFAULT_COMP_MIN;
        this.compMax    = DEFAULT_COMP_MAX;
    }

    // ──────────────────────────────── 정규화 ────────────────────────────────

    /**
     * 값을 [0, 1]로 Min-Max 정규화.
     * max == min인 특수 케이스는 0.5를 반환 (중립값).
     */
    public double normalize(double value, double min, double max) {
        if (max == min) return 0.5;
        double ratio = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    /** (P2-1) 인스턴스 필드 기반 도메인 편의 메서드 — 유동인구 정규화. */
    public double normalizeTraffic(long value) {
        return normalize(value, trafficMin, trafficMax);
    }

    /** (P2-1) 인스턴스 필드 기반 도메인 편의 메서드 — 추정매출 정규화. */
    public double normalizeSales(long value) {
        return normalize(value, salesMin, salesMax);
    }

    /** (P2-1) 인스턴스 필드 기반 도메인 편의 메서드 — 경쟁점포 정규화. */
    public double normalizeCompetition(int value) {
        return normalize(value, compMin, compMax);
    }

    // ──────────────────────────────── 보정계수 계산 ────────────────────────────────

    /**
     * 입지 보정계수 계산 (소수 3자리로 반올림, 0.8~1.2로 클램핑).
     *
     * @deprecated 신규 코드는 {@link #calculateFromScores(double, double, double)} 사용 권장.
     */
    @Deprecated
    public double calculate(long traffic, long sales, int competition) {
        double t = normalizeTraffic(traffic);
        double s = normalizeSales(sales);
        double c = normalizeCompetition(competition);
        return calculateFromScores(t, s, c);
    }

    /**
     * (P4-1) 이미 정규화된 점수(0~1)에서 직접 multiplier를 계산.
     */
    public double calculateFromScores(double trafficScore, double salesScore, double competitionScore) {
        double raw = (WEIGHT_TRAFFIC * trafficScore)
                   + (WEIGHT_SALES   * salesScore)
                   + (WEIGHT_COMPETITION * competitionScore);
        double multiplier = 0.8 + 0.4 * ((raw + 0.2) / 1.0);
        double clamped = Math.max(MULTIPLIER_MIN, Math.min(MULTIPLIER_MAX, multiplier));
        return Math.round(clamped * 1000.0) / 1000.0;
    }

    /** raw_score 자체를 반환 (Response DTO에 표시용, 소수 3자리 반올림). */
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
