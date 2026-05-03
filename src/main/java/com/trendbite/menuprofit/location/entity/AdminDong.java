package com.trendbite.menuprofit.location.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 행정동 경계 엔티티
 * 서울시 행정동 GeoJSON 파일에서 적재된 행정동 경계 정보를 저장한다.
 * 좌표계: EPSG:5179 (Korea 2000 Central Belt 2010) — 상권 EPSG:5181과 다름 주의
 */
@Entity
@Table(
        name = "admin_dong",
        indexes = {
                @Index(name = "idx_admin_dong_code", columnList = "dong_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AdminDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 행정동 코드 (ADM_CD / ADM_SECT_C / ADSTRD_CD — 파일 스키마에 따라 상이)
     * 서울시 추정매출-행정동 API 호출 시 key로 사용된다.
     */
    @Column(name = "dong_code", nullable = false, length = 32)
    private String dongCode;

    /**
     * 행정동명 (ADM_NM / ADSTRD_NM)
     * 예: "안암동", "종로1·2·3·4가동"
     */
    @Column(name = "dong_name", length = 128)
    private String dongName;

    @Lob
    @Column(name = "polygon_wkt", nullable = false, columnDefinition = "LONGTEXT")
    private String polygonWkt;

    /**
     * 좌표계 식별자 (참고용)
     * 기본값 "EPSG:5179"
     */
    @Column(name = "crs", length = 16)
    private String crs;

    public AdminDong(String dongCode, String dongName, String polygonWkt, String crs) {
        this.dongCode = dongCode;
        this.dongName = dongName;
        this.polygonWkt = polygonWkt;
        this.crs = crs;
    }
}
