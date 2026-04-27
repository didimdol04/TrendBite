package com.trendbite.menuprofit.controller;

import com.trendbite.menuprofit.dto.request.SimulationRequestDto;
import com.trendbite.menuprofit.dto.response.SimulationResponseDto;
import com.trendbite.menuprofit.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * POST /simulation/run
     * 입지·트렌드 반영 최종 시뮬레이션 → 경고 레벨 포함 반환
     */
    @PostMapping("/run")
    public ResponseEntity<SimulationResponseDto> run(
            @Valid @RequestBody SimulationRequestDto request) {
        return ResponseEntity.ok(simulationService.simulate(request));
    }
}
