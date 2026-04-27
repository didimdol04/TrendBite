package com.trendbite.menuprofit.trend.exception;

/**
 * FastAPI(트렌드 분석/리포트 생성 서버) 호출 실패를 감싸는 도메인 예외.
 * 글로벌 예외 처리에서 503으로 매핑한다.
 */
public class FastApiCallException extends RuntimeException {

    public FastApiCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public FastApiCallException(String message) {
        super(message);
    }
}
