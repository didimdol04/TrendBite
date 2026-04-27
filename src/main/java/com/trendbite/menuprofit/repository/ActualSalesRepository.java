package com.trendbite.menuprofit.repository;

import com.trendbite.menuprofit.entity.ActualSales;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ActualSalesRepository extends JpaRepository<ActualSales, Long> {
    List<ActualSales> findByMenuIdOrderBySalesDateAsc(Long menuId);
    boolean existsByMenuIdAndSalesDate(Long menuId, LocalDate salesDate);
}