package com.trendbite.menuprofit.repository;

import com.trendbite.menuprofit.entity.SimulationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationResultRepository extends JpaRepository<SimulationResult, Long> {

    List<SimulationResult> findByMenuId(Long menuId);

    // 특정 메뉴의 가장 최근 시뮬레이션 결과 조회
    Optional<SimulationResult> findTopByMenuIdOrderBySimulatedAtDesc(Long menuId);
}
