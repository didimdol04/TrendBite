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
 * - POST /api/v1/location/analyze
 *   Request : LocationAnalysisRequest (address, lat, lng)
 *   Response: LocationAnalysisResponse (locationInfo, rawMetrics, processedScores, finalResult)
 *
 * 핵심 응답 필드: data.finalResult.locationMultiplier (Double, 0.80~1.20)
 * 이 값이 /simulation/run SimulationRequestDto.locationScore 로 전달된다.
 *
 * v3 변경 사항 (round 2)
 * 
 *   (P4-3) {@code IllegalArgumentException} catch 제거 → {@code GlobalExceptionHandler}로 위임
 *              (다른 컨트롤러와 응답 형식 일관성 확보 — 표준 {success:false, error:"BAD_REQUEST"} 본문 반환).
 *              {@code Exception} catch는 fallback 정책상 그대로 유지.
 * 
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationAnalysisService locationAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<LocationAnalysisResponse> analyze(
            @Valid @RequestBody LocationAnalysisRequest request) {

        // (P4-3) IllegalArgumentException은 GlobalExceptionHandler로 위임하기 위해 별도 catch 안 함
        // 다른 컨트롤러(Finance/Simulation)와 응답 형식 일관성 확보:
        //   { "success": false, "error": "BAD_REQUEST", "message": "..." }
        try {
            return ResponseEntity.ok(locationAnalysisService.analyze(request));
        } catch (IllegalArgumentException e) {
            // 명시적으로 다시 throw — GlobalExceptionHandler에 위임
            throw e;
        } catch (Exception e) {
            // (보존) 서비스 다운 시에도 SimulationService의 null→1.0 방어 정책과 일관성 유지를 위해
            //         200 + multiplier=1.0 fallback 응답으로 변환.
            log.error("[LocationController] 예기치 않은 예외, fallback 반환: {}", e.getMessage(), e);
            return ResponseEntity.ok(locationAnalysisService.buildFallbackResponse(
                    request, "입지 분석 중 일시적 오류가 발생했습니다. 기본값(보정계수 1.0)이 적용됩니다."));
        }
    }
}
