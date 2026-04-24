package com.trendbite.menuprofit.location.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendbite.menuprofit.global.exception.GlobalExceptionHandler;
import com.trendbite.menuprofit.location.dto.request.LocationAnalysisRequest;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.DataDto;
import com.trendbite.menuprofit.location.dto.response.LocationAnalysisResponse.FinalResultDto;
import com.trendbite.menuprofit.location.service.LocationAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LocationController 웹 레이어 테스트.
 *
 * @WebMvcTest로 Controller + GlobalExceptionHandler만 로드한다.
 * Service는 @MockitoBean으로 주입.
 */
@WebMvcTest(controllers = LocationController.class)
@Import(GlobalExceptionHandler.class)
class LocationControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;
    @MockitoBean  private LocationAnalysisService locationAnalysisService;

    @Test
    @DisplayName("POST /api/location/analyze — 정상 요청 → 200 + locationMultiplier 반환")
    void testAnalyze_success() throws Exception {
        given(locationAnalysisService.analyze(any())).willReturn(
                LocationAnalysisResponse.builder()
                        .success(true)
                        .data(DataDto.builder()
                                .finalResult(FinalResultDto.builder()
                                        .locationMultiplier(1.082)
                                        .grade(4)
                                        .build())
                                .build())
                        .build()
        );

        LocationAnalysisRequest body = new LocationAnalysisRequest(
                "서울 성북구 안암동", 37.589, 127.032);

        mockMvc.perform(post("/api/location/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.finalResult.locationMultiplier").value(1.082))
                .andExpect(jsonPath("$.data.finalResult.grade").value(4));
    }

    @Test
    @DisplayName("POST /api/location/analyze — lat이 null이면 400 Bad Request")
    void testAnalyze_missingLat() throws Exception {
        String invalidBody = "{\"address\":\"서울\",\"lng\":127.0}";

        mockMvc.perform(post("/api/location/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.lat").exists());
    }

    @Test
    @DisplayName("POST /api/location/analyze — 서비스 예외 시 fallback 응답 (locationMultiplier=1.0)")
    void testAnalyze_serviceException() throws Exception {
        given(locationAnalysisService.analyze(any())).willThrow(new RuntimeException("외부 API 다운"));
        given(locationAnalysisService.buildFallbackResponse(any(), any())).willReturn(
                LocationAnalysisResponse.builder()
                        .success(true)
                        .data(DataDto.builder()
                                .finalResult(FinalResultDto.builder()
                                        .locationMultiplier(1.0)
                                        .grade(5)
                                        .build())
                                .message("입지 분석 중 일시적 오류가 발생했습니다. 기본값(보정계수 1.0)이 적용됩니다.")
                                .build())
                        .build()
        );

        LocationAnalysisRequest body = new LocationAnalysisRequest(
                "서울 강남구", 37.5, 127.0);

        mockMvc.perform(post("/api/location/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalResult.locationMultiplier").value(1.0));
    }
}
