package com.trendbite.menuprofit.controller;

import com.trendbite.menuprofit.dto.request.FinanceRequestDto;
import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /**
     * POST /finance/calculate
     * 원가 계산 → 영업이익·BEP·이익률 반환
     */
    @PostMapping("/calculate")
    public ResponseEntity<FinanceResponseDto> calculate(
            @Valid @RequestBody FinanceRequestDto request) {
        return ResponseEntity.ok(financeService.calculate(request));
    }
}
