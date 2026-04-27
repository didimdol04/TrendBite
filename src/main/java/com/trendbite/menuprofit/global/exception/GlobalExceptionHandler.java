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

    /** @Valid 검증 실패 — null lat/lng 등 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "VALIDATION_FAILED");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /** 서울 외 좌표 — 현재는 throw되지 않지만 정책 변경 시 활성화 */
    @ExceptionHandler(LocationOutOfSeoulException.class)
    public ResponseEntity<Map<String, Object>> handleOutOfSeoul(LocationOutOfSeoulException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "LOCATION_OUT_OF_SEOUL");
        body.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /** FastAPI(트렌드/리포트) 호출 실패 — 503 Service Unavailable */
    @ExceptionHandler(FastApiCallException.class)
    public ResponseEntity<Map<String, Object>> handleFastApi(FastApiCallException e) {
        log.error("[GlobalExceptionHandler] FastAPI 호출 실패: {}", e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "TREND_SERVICE_UNAVAILABLE");
        body.put("message", "트렌드 분석 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /** 그 외 예기치 않은 예외 — 로그 남기고 500 반환 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("[GlobalExceptionHandler] 예기치 않은 예외: {}", e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "INTERNAL_SERVER_ERROR");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
