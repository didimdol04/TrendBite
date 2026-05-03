package com.trendbite.menuprofit.location.repository;

import com.trendbite.menuprofit.location.entity.AdminDong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminDongRepository extends JpaRepository<AdminDong, Long> {

    Optional<AdminDong> findByDongCode(String dongCode);

    boolean existsByDongCode(String dongCode);
}
