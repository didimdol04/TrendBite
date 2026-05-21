package com.trendbite.menuprofit.trend.dto.response;

import com.trendbite.menuprofit.trend.entity.TrendPattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TrendPattern 엔티티의 외부 노출용 DTO.
 *
 * 시계열 데이터(series_json, seriesLength)는 마이크로서비스 분리 원칙상
 * Spring 에서 노출하지 않는다 — DTW 계산은 FastAPI 책임. Spring 은 lifecycle
 * 라벨과 패턴명만 전달한다 (UI 메타데이터는 별도 /lifecycles 엔드포인트).
 * DB 컬럼은 deprecated 데이터로 그대로 유지 (마이그레이션 부담 회피).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrendPatternResponse {

    private String name;
    private String lifecycle;

    public static TrendPatternResponse from(TrendPattern entity) {
        return TrendPatternResponse.builder()
                .name(entity.getName())
                .lifecycle(entity.getLifecycle())
                .build();
    }
}