package com.trendbite.menuprofit.location.controller;

import com.trendbite.menuprofit.location.service.PipMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * (P3-1) 입지 도메인 관리자용 컨트롤러.
 *
 * <p>운영 중에 GeoJSON 데이터가 갱신됐을 때 서버 재기동 없이 PIP 캐시를 새로고침하기 위한 엔드포인트.
 * GeoJSON은 보통 연 1~2회만 변경되므로 일상적 사용은 아니며, 데이터 업데이트 직후에만 호출.
 *
 * <p>현재는 인증 없이 노출되어 있으나, 운영 환경에서는 다음 중 하나로 보호 권장:
 * <ul>
 *   <li>Spring Security로 ADMIN 역할 제한</li>
 *   <li>Nginx/Gateway 레벨에서 IP whitelist</li>
 *   <li>관리자 API Key 헤더 검증 인터셉터</li>
 * </ul>
 *
 * <h3>API</h3>
 * <pre>
 * POST /api/v1/admin/location/cache/invalidate?rebuild=false
 *   - rebuild=false (기본): 캐시만 비움 — 다음 요청에서 lazy 빌드 (~5초 지연 발생)
 *   - rebuild=true:  비운 직후 즉시 재빌드 — 사용자 요청에 영향 없음
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/location")
@RequiredArgsConstructor
public class LocationAdminController {

    private final PipMappingService pipMappingService;

    @PostMapping("/cache/invalidate")
    public ResponseEntity<Map<String, Object>> invalidateCache(
            @RequestParam(defaultValue = "false") boolean rebuild) {

        log.info("[LocationAdmin] 캐시 무효화 요청 (rebuild={})", rebuild);
        long start = System.currentTimeMillis();

        if (rebuild) {
            pipMappingService.invalidateAndRebuild();
        } else {
            pipMappingService.invalidateCache();
        }

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("rebuild", rebuild);
        body.put("elapsedMs", elapsed);
        body.put("message", rebuild
                ? "PIP 캐시 무효화 및 재빌드 완료"
                : "PIP 캐시 무효화 완료 (다음 요청 시 lazy 빌드 — 약 5초 지연 예상)");
        return ResponseEntity.ok(body);
    }
}
