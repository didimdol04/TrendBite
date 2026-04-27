package com.trendbite.menuprofit.location.repository;

import com.trendbite.menuprofit.location.entity.CommercialArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommercialAreaRepository extends JpaRepository<CommercialArea, Long> {

    /** 상권 코드로 단건 조회 (B-3 API 호출 시 area_name 참조용) */
    Optional<CommercialArea> findByAreaCode(String areaCode);

    // 행정동 코드로 상권 조회 (fallback용)
    Optional<CommercialArea> findFirstByAdminDongCode(String adminDongCode);

    /** Seed 초기 로딩 시 중복 방지 체크 */
    boolean existsByAreaCode(String areaCode);
}
