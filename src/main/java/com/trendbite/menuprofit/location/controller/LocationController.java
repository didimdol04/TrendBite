package com.trendbite.menuprofit.location.controller;

import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.service.LocationAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 입지 분석 REST 컨트롤러.
 *
 * - POST /api/location/analyze
 *   Request : LocationAnalysisRequest (address, lat, lng)
 *   Response: LocationAnalysisResponse (locationInfo, rawMetrics, processedScores, finalResult)
 *
 * 핵심 응답 필드: data.finalResult.locationMultiplier (Double, 0.80~1.20)
 * 이 값이 /simulation/run SimulationRequestDto.locationScore 로 전달된다.
 */
@Slf4j
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationAnalysisService locationAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<LocationAnalysisResponse> analyze(
            @Valid @RequestBody LocationAnalysisRequest request) {
        try {
            LocationAnalysisResponse response = locationAnalysisService.analyze(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[LocationController] 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // 예기치 않은 예외 → 200 + locationMultiplier=1.0 fallback
            // (SimulationService와 동일한 null→1.0 방어 로직과 일치시킴)
            log.error("[LocationController] 예기치 않은 예외, fallback 반환: {}", e.getMessage(), e);
            return ResponseEntity.ok(locationAnalysisService.buildFallbackResponse(
                    request, "입지 분석 중 일시적 오류가 발생했습니다. 기본값(보정계수 1.0)이 적용됩니다."));
        }
    }
}
