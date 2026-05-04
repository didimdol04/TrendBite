package com.trendbite.menuprofit.global.exception;

import com.trendbite.menuprofit.location.exception.LocationOutOfSeoulException;
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

    /** 400 - @Valid 검증 실패 (null lat/lng 등) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(buildBody("VALIDATION_FAILED", fieldErrors));
    }

    /** 400 - 잘못된 요청 (메뉴 없음, 날짜 중복 등) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(buildBody("BAD_REQUEST", e.getMessage()));
    }

    /** 422 - 처리 불가 (데이터 부족 등 비즈니스 규칙 위반) */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.unprocessableEntity().body(buildBody("UNPROCESSABLE_ENTITY", e.getMessage()));
    }

    /** 400 - 서울 외 좌표 (정책 변경 시 활성화) */
    @ExceptionHandler(LocationOutOfSeoulException.class)
    public ResponseEntity<Map<String, Object>> handleOutOfSeoul(LocationOutOfSeoulException e) {
        return ResponseEntity.badRequest().body(buildBody("LOCATION_OUT_OF_SEOUL", e.getMessage()));
    }

    /** 500 - 예기치 않은 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("[GlobalExceptionHandler] 예기치 않은 예외: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildBody("INTERNAL_SERVER_ERROR", e.getMessage()));
    }

    /** 응답 본문 통일 */
    private Map<String, Object> buildBody(String errorCode, Object payload) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", errorCode);
        if (payload instanceof Map) {
            body.put("fieldErrors", payload);
        } else {
            body.put("message", payload);
        }
        return body;
    }
}