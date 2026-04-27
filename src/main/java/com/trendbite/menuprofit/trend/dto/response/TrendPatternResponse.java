package com.trendbite.menuprofit.trend.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendbite.menuprofit.trend.entity.TrendPattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrendPatternResponse {

    /** 정적 팩토리 내부 파싱 전용. ObjectMapper는 thread-safe. */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Integer>> INT_LIST_TYPE = new TypeReference<>() {};

    private String name;
    private String lifecycle;
    /** 월별 관심도 시계열 (이미 파싱된 형태로 노출) */
    private List<Integer> series;
    private Integer seriesLength;

    public static TrendPatternResponse from(TrendPattern entity) {
        List<Integer> series;
        try {
            series = entity.getSeriesJson() == null
                    ? Collections.emptyList()
                    : MAPPER.readValue(entity.getSeriesJson(), INT_LIST_TYPE);
        } catch (Exception e) {
            // 데이터 손상 시 빈 배열 폴백 — 프론트가 빈 배열로 안전하게 렌더링.
            series = Collections.emptyList();
        }
        return TrendPatternResponse.builder()
                .name(entity.getName())
                .lifecycle(entity.getLifecycle())
                .series(series)
                .seriesLength(entity.getSeriesLength())
                .build();
    }
}
