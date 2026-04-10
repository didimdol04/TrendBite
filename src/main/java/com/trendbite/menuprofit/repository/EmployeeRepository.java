package com.trendbite.menuprofit.repository;

import com.trendbite.menuprofit.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByMenuId(Long menuId);
}