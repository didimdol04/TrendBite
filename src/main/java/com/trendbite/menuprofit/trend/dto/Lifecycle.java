package com.trendbite.menuprofit.trend.dto;

import java.util.Arrays;

/**
 * 트렌드 라이프사이클 분류 + UI 메타데이터.
 *
 * FastAPI similar_patterns 응답의 lifecycle 한글 값과 매핑된다.
 * 프론트엔드 (Next.js 등) 가 색상/설명을 별도로 관리하지 않도록 백엔드 단일 소스.
 *
 * 도메인 출처: FastAPI trend.py PAST_PATTERNS (7개 시드, 6개 unique lifecycle).
 */
public enum Lifecycle {

    HOCKEY_STICK    ("단기급락",   "정점 후 빠르게 식는 유행",          "#E74C3C"),
    FADING          ("유행후정체", "정점 후 안정 없이 점진 감소",       "#7F8C8D"),
    SEASONAL        ("계절성",     "매년 동일 시기 반복",                "#3498DB"),
    NICHE_SUCCESS   ("안정정착",   "정점 후 일부 매장에서 안정",         "#16A085"),
    LONG_TERM_STABLE("지속성장",   "장기 정착 가능성",                   "#27AE60"),
    REBOUND         ("재유행",     "주기적 재부상 패턴",                 "#9B59B6");

    private final String label;        // FastAPI 응답의 한글 값과 정확히 일치
    private final String description;
    private final String color;        // HEX 색상 코드 (프론트 신호등/뱃지용)

    Lifecycle(String label, String description, String color) {
        this.label = label;
        this.description = description;
        this.color = color;
    }

    public String getLabel()       { return label; }
    public String getDescription() { return description; }
    public String getColor()       { return color; }

    /**
     * FastAPI 응답의 한글 lifecycle 문자열을 enum 으로 역매핑.
     * 미등록 값은 null 반환 (예외 던지지 않음 — 신규 도메인 값 호환 유지).
     */
    public static Lifecycle fromLabel(String label) {
        if (label == null) return null;
        return Arrays.stream(values())
                .filter(lc -> lc.label.equals(label))
                .findFirst()
                .orElse(null);
    }
}