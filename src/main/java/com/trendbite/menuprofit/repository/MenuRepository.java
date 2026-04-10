package com.trendbite.menuprofit.repository;

import com.trendbite.menuprofit.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}