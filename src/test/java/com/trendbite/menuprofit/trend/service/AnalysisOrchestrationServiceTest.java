package com.trendbite.menuprofit.trend.service;

import com.trendbite.menuprofit.dto.request.FinanceRequestDto;
import com.trendbite.menuprofit.dto.request.SimulationRequestDto;
import com.trendbite.menuprofit.dto.request.sub.SalesAssumptionDto;
import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import com.trendbite.menuprofit.dto.response.SimulationResponseDto;
import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.service.LocationAnalysisService;
import com.trendbite.menuprofit.repository.MenuRepository;
import com.trendbite.menuprofit.service.FinanceService;
import com.trendbite.menuprofit.service.SimulationService;
import com.trendbite.menuprofit.trend.client.FastApiReportClient;
import com.trendbite.menuprofit.trend.dto.request.AnalysisRequest;
import com.trendbite.menuprofit.trend.dto.response.AnalysisResponse;
import com.trendbite.menuprofit.trend.dto.response.ReportResponse;
import com.trendbite.menuprofit.trend.dto.response.TrendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnalysisOrchestrationService 회귀 테스트.
 *
 * 주요 검증 목적:
 *  - FastAPI trend_score(0~100) → SimulationService trendScore(0.0~1.0) 단위 변환
 *  - trend_score null 시 0.5 fallback (SimulationService 내부 default 와 일치)
 *  - 통합 응답 data.simulation 필드 채워짐
 *
 * 협업자 SimulationService 시그니처 변경 없이 입력 어댑팅이 정확한지 보장한다.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisOrchestrationServiceTest {

    @Mock LocationAnalysisService locationAnalysisService;
    @Mock FinanceService          financeService;
    @Mock SimulationService       simulationService;
    @Mock TrendService            trendService;
    @Mock FastApiReportClient     fastApiReportClient;
    @Mock MenuRepository          menuRepository;

    @InjectMocks AnalysisOrchestrationService orchestrationService;

    @Test
    @DisplayName("trend_score=70 → SimulationService 가 0.7 받는지 단위 변환 검증")
    void unitConversion_70_normalizesToZeroPointSeven() {
        // given
        stubAllDependencies(70);

        // when
        orchestrationService.run(buildMinimalRequest());

        // then: SimulationService 호출 인자의 trendScore 가 0.7 인지
        ArgumentCaptor<SimulationRequestDto> captor =
                ArgumentCaptor.forClass(SimulationRequestDto.class);
        verify(simulationService).simulate(captor.capture());
        assertThat(captor.getValue().getTrendScore()).isEqualTo(0.7);
    }

    @Test
    @DisplayName("trend_score=null → SimulationService 가 0.5 fallback 받는지")
    void unitConversion_nullFallsBackToZeroPointFive() {
        // given
        stubAllDependencies(null);

        // when
        orchestrationService.run(buildMinimalRequest());

        // then
        ArgumentCaptor<SimulationRequestDto> captor =
                ArgumentCaptor.forClass(SimulationRequestDto.class);
        verify(simulationService).simulate(captor.capture());
        assertThat(captor.getValue().getTrendScore()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("응답 data.simulation 이 SimulationService 반환값과 동일")
    void responseContainsSimulationResult() {
        // given
        SimulationResponseDto simStub = SimulationResponseDto.builder().success(true).build();
        when(locationAnalysisService.analyze(any())).thenReturn(buildLocation(0.95));
        when(financeService.calculate(any())).thenReturn(buildFinance());
        when(trendService.analyze(any())).thenReturn(buildTrend(80));
        when(simulationService.simulate(any())).thenReturn(simStub);
        when(fastApiReportClient.generate(any())).thenReturn(buildReport());

        // when
        AnalysisResponse resp = orchestrationService.run(buildMinimalRequest());

        // then: 통합 응답에 simulation 결과 그대로 들어있어야 함
        assertThat(resp.getData().getSimulation()).isSameAs(simStub);
    }

    // ── helpers ──────────────────────────────────────────────────

    private void stubAllDependencies(Integer trendScore) {
        when(locationAnalysisService.analyze(any())).thenReturn(buildLocation(0.95));
        when(financeService.calculate(any())).thenReturn(buildFinance());
        when(trendService.analyze(any())).thenReturn(buildTrend(trendScore));
        when(simulationService.simulate(any())).thenReturn(SimulationResponseDto.builder().build());
        when(fastApiReportClient.generate(any())).thenReturn(buildReport());
    }

    private AnalysisRequest buildMinimalRequest() {
        SalesAssumptionDto sales = new SalesAssumptionDto();
        sales.setTrendDurationWeeks(12);

        FinanceRequestDto finance = new FinanceRequestDto();
        finance.setSalesAssumption(sales);

        LocationAnalysisRequest loc = new LocationAnalysisRequest();
        loc.setLat(37.5);
        loc.setLng(127.0);

        return AnalysisRequest.builder()
                .keyword("마라탕")
                .category("dining")
                .location(loc)
                .finance(finance)
                .build();
    }

    private LocationAnalysisResponse buildLocation(double multiplier) {
        return LocationAnalysisResponse.builder()
                .success(true)
                .data(LocationAnalysisResponse.DataDto.builder()
                        .finalResult(LocationAnalysisResponse.FinalResultDto.builder()
                                .locationMultiplier(multiplier)
                                .grade(7)
                                .build())
                        .build())
                .build();
    }

    private FinanceResponseDto buildFinance() {
        return FinanceResponseDto.builder()
                .success(true)
                .data(FinanceResponseDto.DataDto.builder()
                        .monthlyResult(FinanceResponseDto.MonthlyResultDto.builder()
                                .operatingProfit(1_000_000L)
                                .profitMargin(0.25)
                                .build())
                        .breakEven(FinanceResponseDto.BreakEvenDto.builder()
                                .breakEvenQuantityPerDay(13)
                                .build())
                        .build())
                .build();
    }

    private TrendResponse buildTrend(Integer trendScore) {
        return TrendResponse.builder()
                .trendScore(trendScore)
                .plcStage("성장기")
                .overallTrend("상승")
                .recentTrend("최근 상승")
                .latestInterest(80.0)
                .averageInterest(70.0)
                .trendSpeed("growing")
                .peakRatio(0.8)
                .growthRate(2.0)
                .recommendation("Good opportunity")
                .build();
    }

    private ReportResponse buildReport() {
        return ReportResponse.builder()
                .verdict("도입 권장")
                .llmReport("...")
                .build();
    }
}