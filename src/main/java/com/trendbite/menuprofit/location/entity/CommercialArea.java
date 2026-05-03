package com.trendbite.menuprofit.location.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상권 영역 엔티티
 * 서울시 상권 GeoJSON 파일에서 적재된 1,650개 상권의 경계 정보를 저장한다.
 * 좌표계: EPSG:5181 (Korea 2000 Central Belt)
 */
@Entity
@Table(
        name = "commercial_area",
        indexes = {
                @Index(name = "idx_commercial_area_code", columnList = "area_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CommercialArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상권 코드 (TRDAR_CD)
     * 서울시 공공데이터 API 호출 시 key로 사용된다.
     */
    @Column(name = "area_code", nullable = false, length = 32)
    private String areaCode;

    /**
     * 상권명 (TRDAR_CD_NM 또는 TRDAR_CD_N)
     * 예: "안암역_1", "홍대입구역"
     */
    @Column(name = "area_name", length = 128)
    private String areaName;

    /**
     * 행정동 코드 (ADSTRD_CD) — 상권 GeoJSON에 포함된 경우만 저장
     * GeoJSON에 없으면 null이며, 이 경우 PIP 매핑은 행정동 테이블 쪽에서 별도로 수행된다.
     */
    @Column(name = "admin_dong_code", length = 32)
    private String adminDongCode;

    /**
     * 폴리곤 WKT (Well-Known Text)
     * MySQL의 TEXT 타입 대신 LONGTEXT 사용 (일부 폴리곤이 매우 길 수 있음).
     */
    @Lob
    @Column(name = "polygon_wkt", nullable = false, columnDefinition = "LONGTEXT")
    private String polygonWkt;

    /**
     * 좌표계 식별자 (참고용)
     * 기본값 "EPSG:5181"
     */
    @Column(name = "crs", length = 16)
    private String crs;

    public CommercialArea(String areaCode, String areaName, String adminDongCode,
                          String polygonWkt, String crs) {
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.adminDongCode = adminDongCode;
        this.polygonWkt = polygonWkt;
        this.crs = crs;
    }
}
