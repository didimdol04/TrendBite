package com.trendbite.menuprofit.dto.request;

import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class SimulationRequestDto {
    private FinanceResponseDto.DataDto financeResult; // /finance/calculate 응답의 data
    private Double locationScore;                     // 입지 보정계수 (0.8~1.2)
    private Double trendScore;                        // 트렌드 점수 (0.0~1.0)
    private Integer trendDurationWeeks;               // 트렌드 지속 기간 (주)
}