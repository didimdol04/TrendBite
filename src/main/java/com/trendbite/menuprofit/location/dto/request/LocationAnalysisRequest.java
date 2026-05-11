package com.trendbite.menuprofit.location.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /api/v1/location/analyze 요청 DTO
 *
 * 프론트엔드에서 Kakao Postcode → Geocoder로 WGS84 좌표를 획득한 뒤 전송한다.
 *
 * <h3>v2 (P1-4): lat/lng 범위 검증 추가</h3>
 * 비정상 값({@code lat=200} 등) 입력 시 즉시 400 응답을 반환하여
 * proj4j 변환 → PIP 매칭 실패 → fallback 흐름까지 도달하지 않게 한다.
 * 좌표 오타(lat/lng 순서 뒤바뀜)도 빠르게 감지된다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationAnalysisRequest {

    /** 사용자 입력 주소 (응답에 echo — 필수 아님) */
    private String address;

    /** 위도 (WGS84) */
    @NotNull(message = "위도(lat)는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0",  message = "위도는 90 이하여야 합니다")
    private Double lat;

    /** 경도 (WGS84) */
    @NotNull(message = "경도(lng)는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0",  message = "경도는 180 이하여야 합니다")
    private Double lng;
}
