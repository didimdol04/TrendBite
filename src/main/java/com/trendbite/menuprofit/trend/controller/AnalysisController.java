package com.trendbite.menuprofit.trend.controller;

import com.trendbite.menuprofit.trend.dto.request.AnalysisRequest;
import com.trendbite.menuprofit.trend.dto.response.AnalysisResponse;
import com.trendbite.menuprofit.trend.service.AnalysisOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisOrchestrationService orchestrationService;

    /** POST /api/v1/analysis/run — 입지/재무/트렌드/리포트 4단계 통합 분석 */
    @PostMapping("/run")
    public ResponseEntity<AnalysisResponse> run(
            @Valid @RequestBody AnalysisRequest request) {
        return ResponseEntity.ok(orchestrationService.run(request));
    }
}
