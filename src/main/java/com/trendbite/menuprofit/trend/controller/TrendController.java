package com.trendbite.menuprofit.trend.controller;

import com.trendbite.menuprofit.trend.dto.request.TrendRequest;
import com.trendbite.menuprofit.trend.dto.response.TrendKeywordResponse;
import com.trendbite.menuprofit.trend.dto.response.TrendPatternResponse;
import com.trendbite.menuprofit.trend.dto.response.TrendResponse;
import com.trendbite.menuprofit.trend.service.TrendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trend")
@RequiredArgsConstructor
public class TrendController {

    private static final int MAX_POPULAR_LIMIT = 50;

    private final TrendService trendService;

    /** POST /api/v1/trend/analyze — 캐시 또는 FastAPI 트렌드 분석 */
    @PostMapping("/analyze")
    public ResponseEntity<TrendResponse> analyze(
            @Valid @RequestBody TrendRequest request) {
        return ResponseEntity.ok(trendService.analyze(request));
    }

    /** GET /api/v1/trend/popular?limit=10 — 검색 카운트 상위 키워드 */
    @GetMapping("/popular")
    public ResponseEntity<List<TrendKeywordResponse>> popular(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_POPULAR_LIMIT);
        List<TrendKeywordResponse> body = trendService.getPopularKeywords(safeLimit).stream()
                .map(TrendKeywordResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** GET /api/v1/trend/patterns — DTW 비교 기준 시드 패턴 전체 */
    @GetMapping("/patterns")
    public ResponseEntity<List<TrendPatternResponse>> patterns() {
        List<TrendPatternResponse> body = trendService.getAllPatterns().stream()
                .map(TrendPatternResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }
}
