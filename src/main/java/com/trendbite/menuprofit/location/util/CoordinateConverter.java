package com.trendbite.menuprofit.location.util;

import jakarta.annotation.PostConstruct;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Component;

/**
 * WGS84 좌표(위·경도)를 서울시 좌표계로 변환하는 유틸리티.
 *
 * 상권 폴리곤: EPSG:5181 (Korea 2000 Central Belt)
 * 행정동 폴리곤: EPSG:5179 (Korea 2000 Central Belt 2010) — 실제 파일 확인 결과 5179
 *
 * proj4j의 CoordinateTransform은 내부 상태를 갖지 않아 thread-safe 하다.
 * 단일 인스턴스로 공유하여 매 요청마다 CRS 생성 비용을 피한다.
 */
@Component
public class CoordinateConverter {

    public static final String EPSG_WGS84 = "EPSG:4326";
    public static final String EPSG_5181  = "EPSG:5181"; // 상권
    public static final String EPSG_5179  = "EPSG:5179"; // 행정동

    private CoordinateTransform toEpsg5181;
    private CoordinateTransform toEpsg5179;

    @PostConstruct
    public void init() {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem wgs84 = factory.createFromName(EPSG_WGS84);
        this.toEpsg5181 = new BasicCoordinateTransform(wgs84, factory.createFromName(EPSG_5181));
        this.toEpsg5179 = new BasicCoordinateTransform(wgs84, factory.createFromName(EPSG_5179));
    }

    /**
     * WGS84 (lat, lng) → EPSG:5181 (상권)
     *
     * 주의: proj4j의 ProjCoordinate는 (x=lng, y=lat) 순서를 사용한다.
     *       서울시 표준: 입력은 (lat, lng), proj4j 내부는 (x=lng, y=lat)
     */
    public ProjCoordinate toEpsg5181(double lat, double lng) {
        ProjCoordinate src = new ProjCoordinate(lng, lat);
        ProjCoordinate dst = new ProjCoordinate();
        toEpsg5181.transform(src, dst);
        return dst;
    }

    /** WGS84 (lat, lng) → EPSG:5179 (행정동) */
    public ProjCoordinate toEpsg5179(double lat, double lng) {
        ProjCoordinate src = new ProjCoordinate(lng, lat);
        ProjCoordinate dst = new ProjCoordinate();
        toEpsg5179.transform(src, dst);
        return dst;
    }
}
