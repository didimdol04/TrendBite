package com.trendbite.menuprofit.location.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PIP 매핑 결과 DTO
 * 특정 좌표에 해당하는 상권 코드(+이름) + 행정동 코드(+이름)를 담는다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AreaMapping {

    private String areaCode;      // 상권 코드 (TRDAR_CD)
    private String areaName;      // 상권명
    private String dongCode;      // 행정동 코드
    private String dongName;      // 행정동명

    /** 편의 생성자: 코드만 있는 경우 (이름은 null) */
    public AreaMapping(String areaCode, String dongCode) {
        this.areaCode = areaCode;
        this.dongCode = dongCode;
    }
}
