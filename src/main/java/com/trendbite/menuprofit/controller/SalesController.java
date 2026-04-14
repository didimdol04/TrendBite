package com.trendbite.menuprofit.controller;

import com.trendbite.menuprofit.dto.request.SalesRecordRequestDto;
import com.trendbite.menuprofit.dto.response.SalesCorrectionResponseDto;
import com.trendbite.menuprofit.dto.response.SalesRecordResponseDto;
import com.trendbite.menuprofit.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    /**
     * POST /sales/record
     * 실판매 기록 저장
     */
    @PostMapping("/record")
    public ResponseEntity<SalesRecordResponseDto> record(
            @Valid @RequestBody SalesRecordRequestDto request) {
        try {
            SalesRecordResponseDto response = salesService.record(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /sales/correction/{menuId}
     * 실판매 데이터 기반 보정된 예측값 조회
     */
    @GetMapping("/correction/{menuId}")
    public ResponseEntity<SalesCorrectionResponseDto> getCorrection(
            @PathVariable Long menuId) {
        try {
            SalesCorrectionResponseDto response = salesService.getCorrection(menuId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // 데이터 부족 (3일 미만)
            return ResponseEntity.badRequest().build();
        }
    }
}
