package com.trendbite.menuprofit.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PIP 매핑 결과 DTO
 *
 * 특정 좌표에 해당하는 상권 코드(+이름) + 행정동 코드(+이름)를 담는다.
 *
 * <h3>v2 (P1-1): fallback 메타데이터 추가</h3>
 * <ul>
 *   <li>{@code nearestFallback}: PIP 정타가 아닌 최근접 폴리곤 fallback이었는지 표시</li>
 *   <li>{@code nearestDistanceMeters}: fallback 시 폴리곤까지 거리 (EPSG:5181 단위 = 미터)</li>
 * </ul>
 * 이 정보는 LocationInfoDto로 전파되어 프론트가
 * "약 OOOm 떨어진 인근 상권 데이터 기반 추정값" 같은 안내를 사용자에게 노출하는 데 사용된다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaMapping {

    private String areaCode;      // 상권 코드 (TRDAR_CD)
    private String areaName;      // 상권명
    private String dongCode;      // 행정동 코드
    private String dongName;      // 행정동명

    /**
     * 상권이 PIP 정타로 매칭되지 않고 반경 fallback으로 결정됐는지 여부.
     * <ul>
     *     <li>{@code false}: 좌표가 폴리곤 내부에 있어 정확히 매칭됨</li>
     *     <li>{@code true}:  폴리곤 외부 좌표 → 반경 2km 최근접 상권 사용</li>
     * </ul>
     */
    private boolean nearestFallback;

    /**
     * fallback 시 점에서 가장 가까운 상권 폴리곤까지 거리(미터, EPSG:5181 기준).
     * PIP 정타이거나 행정동만 매핑된 경우 {@code null}.
     */
    private Double nearestDistanceMeters;

    // ──────────────────────────────────────────────────────────────
    // 편의 생성자: 기존 호출자(테스트 코드 포함) 호환을 위해 명시 유지
    // ──────────────────────────────────────────────────────────────

    /** 코드만 있는 경우 (이름·메타데이터는 null/false) — fallback flag false로 초기화 */
    public AreaMapping(String areaCode, String dongCode) {
        this.areaCode = areaCode;
        this.dongCode = dongCode;
        this.nearestFallback = false;
    }

    /** 4-arg: 코드 + 이름 (메타데이터 없음, fallback flag false) — 기존 PipMappingService/테스트 호환 */
    public AreaMapping(String areaCode, String areaName, String dongCode, String dongName) {
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.dongCode = dongCode;
        this.dongName = dongName;
        this.nearestFallback = false;
    }
}
