package com.trendbite.menuprofit.trend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrendRequest {

    @NotBlank
    private String keyword;

    /** 사용자가 명시적으로 골라야 하는 값 — default 두면 오분류 사일런트 버그. */
    @NotBlank
    private String category;
}
