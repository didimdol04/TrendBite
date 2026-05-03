package com.trendbite.menuprofit.location.exception;

/**
 * 입력 좌표가 서울시 상권/행정동 경계 밖일 때 발생시키는 커스텀 예외.
 *
 * 현재 구현은 LocationAnalysisService에서 Optional.empty() → buildFallbackResponse()로
 * 전환하여 대부분의 경우 이 예외는 throw되지 않는다.
 * 다만 향후 "서울 외에는 명시적으로 400 반환" 정책으로 변경할 때를 대비해 남겨둔다.
 */
public class LocationOutOfSeoulException extends RuntimeException {

    public LocationOutOfSeoulException(String message) {
        super(message);
    }

    public LocationOutOfSeoulException(double lat, double lng) {
        super(String.format("서울시 경계 밖 좌표입니다. (lat=%.6f, lng=%.6f)", lat, lng));
    }
}
