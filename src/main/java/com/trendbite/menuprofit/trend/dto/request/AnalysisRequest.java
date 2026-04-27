package com.trendbite.menuprofit.trend.dto.request;

import com.trendbite.menuprofit.dto.request.FinanceRequestDto;
import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프론트엔드 메인 진입점. POST /api/v1/analysis/run 의 바디.
 *
 * 한 번의 호출로 Location → Finance → Trend → Report 4단계를 오케스트레이션한다.
 * 각 필드는 기존 도메인 DTO를 그대로 재사용 (단일 진실 소스).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisRequest {

    @NotBlank
    private String keyword;

    @NotBlank
    private String category;

    @NotNull
    @Valid
    private LocationAnalysisRequest location;

    @NotNull
    @Valid
    private FinanceRequestDto finance;
}
