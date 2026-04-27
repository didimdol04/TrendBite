package com.trendbite.menuprofit.global.exception;

import com.trendbite.menuprofit.location.exception.LocationOutOfSeoulException;
import com.trendbite.menuprofit.trend.exception.FastApiCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @Valid 검증 실패 — 필드별 에러 맵 포함 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = errorBody("VALIDATION_FAILED", "입력값이 올바르지 않습니다.");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /** 잘못된 요청 (메뉴 없음, 날짜 중복 등) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(errorBody("BAD_REQUEST", e.getMessage()));
    }

    /** 처리 불가 (데이터 부족 등 비즈니스 규칙 위반) */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleUnprocessable(IllegalStateException e) {
        return ResponseEntity.unprocessableEntity().body(errorBody("UNPROCESSABLE", e.getMessage()));
    }

    /** 서울 외 좌표 — 정책 변경 시 활성화 */
    @ExceptionHandler(LocationOutOfSeoulException.class)
    public ResponseEntity<Map<String, Object>> handleOutOfSeoul(LocationOutOfSeoulException e) {
        return ResponseEntity.badRequest().body(errorBody("LOCATION_OUT_OF_SEOUL", e.getMessage()));
    }

    /** FastAPI(트렌드/리포트) 호출 실패 — 503 Service Unavailable */
    @ExceptionHandler(FastApiCallException.class)
    public ResponseEntity<Map<String, Object>> handleFastApi(FastApiCallException e) {
        log.error("[GlobalExceptionHandler] FastAPI 호출 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody("TREND_SERVICE_UNAVAILABLE",
                        "트렌드 분석 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요."));
    }

    /** 그 외 예기치 않은 예외 — 메시지는 안전한 고정 문자열 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("[GlobalExceptionHandler] 예기치 않은 예외: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_SERVER_ERROR",
                        "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    private Map<String, Object> errorBody(String errorCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", errorCode);
        body.put("message", message);
        return body;
    }
}
