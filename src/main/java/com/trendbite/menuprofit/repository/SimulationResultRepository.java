package com.trendbite.menuprofit.repository;

import com.trendbite.menuprofit.entity.SimulationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SimulationResultRepository extends JpaRepository<SimulationResult, Long> {
    List<SimulationResult> findByMenuId(Long menuId);
}