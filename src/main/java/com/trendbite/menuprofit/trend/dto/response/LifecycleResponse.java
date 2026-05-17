package com.trendbite.menuprofit.trend.dto.response;

import com.trendbite.menuprofit.trend.dto.Lifecycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lifecycle enum 의 외부 노출용 DTO.
 *
 * 응답 예시:
 *   { "name": "단기급락", "description": "정점 후 빠르게 식는 유행", "color": "#E74C3C" }
 *
 * 프론트엔드는 trend analyze 응답의 lifecycle 한글값을 key 로 매칭해서
 * description/color 를 사용한다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LifecycleResponse {

    private String name;          // 한글 라벨 (FastAPI 응답값과 일치)
    private String description;
    private String color;

    public static LifecycleResponse from(Lifecycle lc) {
        return LifecycleResponse.builder()
                .name(lc.getLabel())
                .description(lc.getDescription())
                .color(lc.getColor())
                .build();
    }
}