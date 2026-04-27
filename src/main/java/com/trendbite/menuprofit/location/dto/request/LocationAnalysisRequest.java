package com.trendbite.menuprofit.location.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /api/location/analyze 요청 DTO
 *
 * 프론트엔드에서 Kakao Postcode → Geocoder로 WGS84 좌표를 획득한 뒤 전송한다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LocationAnalysisRequest {

    /** 사용자 입력 주소 (응답에 echo — 필수 아님) */
    private String address;

    /** 위도 (WGS84) */
    @NotNull
    private Double lat;

    /** 경도 (WGS84) */
    @NotNull
    private Double lng;
}
