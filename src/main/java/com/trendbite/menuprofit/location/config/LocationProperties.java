package com.trendbite.menuprofit.location.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * (P2-1) 입지 보정계수 Min-Max 기준값 외부화.
 *
 * <p>기존에는 {@code LocationCalculatorService} 내부에 {@code static final} 상수로 하드코딩되어 있었으나,
 * 실데이터 수집 후 5/95 percentile 기준으로 조정 가능하도록 외부 properties로 분리.
 *
 * <h3>적용 키 (application.properties)</h3>
 * <pre>
 * location.calculator.traffic-min=5000
 * location.calculator.traffic-max=40000
 * location.calculator.sales-min=10000000
 * location.calculator.sales-max=50000000
 * location.calculator.comp-min=0
 * location.calculator.comp-max=30
 * </pre>
 *
 * <p>설정 누락 시 기본값(필드 초기화)은 명세 v2.0의 초기 추정치를 그대로 사용한다.
 * 따라서 properties에 키가 없어도 기존 동작과 동일하게 작동.
 */
@Component
@ConfigurationProperties(prefix = "location.calculator")
@Getter
@Setter
public class LocationProperties {

    /** 유동인구 Min-Max (명) */
    private double trafficMin = 5_000.0;
    private double trafficMax = 40_000.0;

    /** 추정매출 Min-Max (원/월) */
    private double salesMin = 10_000_000.0;
    private double salesMax = 50_000_000.0;

    /** 경쟁점포 수 Min-Max (개) */
    private int compMin = 0;
    private int compMax = 30;
}
