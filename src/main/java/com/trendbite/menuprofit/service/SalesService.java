package com.trendbite.menuprofit.service;

import com.trendbite.menuprofit.dto.request.SalesRecordRequestDto;
import com.trendbite.menuprofit.dto.response.SalesCorrectionResponseDto;
import com.trendbite.menuprofit.dto.response.SalesRecordResponseDto;
import com.trendbite.menuprofit.entity.ActualSales;
import com.trendbite.menuprofit.entity.Menu;
import com.trendbite.menuprofit.repository.ActualSalesRepository;
import com.trendbite.menuprofit.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private static final int MIN_DATA_DAYS = 3; // 보정값 신뢰 최소 데이터 수

    private final ActualSalesRepository actualSalesRepository;
    private final MenuRepository menuRepository;

    @Transactional
    public SalesRecordResponseDto record(SalesRecordRequestDto req) {

        // 음수 값 검증
        if (req.getActualQuantity() < 0) {
            throw new IllegalArgumentException("실제 판매량은 0 이상이어야 합니다.");
        }
        if (req.getActualRevenue() < 0) {
            throw new IllegalArgumentException("실제 매출은 0 이상이어야 합니다.");
        }

        Menu menu = menuRepository.findById(req.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다. id=" + req.getMenuId()));

        // TODO: 날짜 중복 체크 (ActualSales에 menu_id + sales_date Unique 제약 추가 후 활성화)

        ActualSales sales = new ActualSales();
        sales.setMenu(menu);
        sales.setSalesDate(req.getSalesDate());
        sales.setActualQuantity(req.getActualQuantity());
        sales.setActualRevenue(req.getActualRevenue());

        ActualSales saved = actualSalesRepository.save(sales);

        return SalesRecordResponseDto.builder()
                .success(true)
                .data(SalesRecordResponseDto.DataDto.builder()
                        .id(saved.getId())
                        .menuId(saved.getMenu().getId())
                        .salesDate(saved.getSalesDate())
                        .actualQuantity(saved.getActualQuantity())
                        .actualRevenue(saved.getActualRevenue())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public SalesCorrectionResponseDto getCorrection(Long menuId) {
        menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다. id=" + menuId));

        List<ActualSales> salesList =
                actualSalesRepository.findByMenuIdOrderBySalesDateAsc(menuId);

        if (salesList.isEmpty()) {
            throw new IllegalArgumentException(
                    "실판매 데이터가 없습니다. menuId=" + menuId);
        }

        // 최소 데이터 수 경고
        if (salesList.size() < MIN_DATA_DAYS) {
            throw new IllegalStateException(
                    "데이터가 부족합니다. 최소 " + MIN_DATA_DAYS + "일 이상 기록 후 조회해주세요."
                            + " (현재 " + salesList.size() + "일)");
        }

        // TODO: 단순 평균 → EWMA(지수 가중 이동 평균)으로 개선 예정
        double avgActualDailySales = salesList.stream()
                .mapToInt(ActualSales::getActualQuantity)
                .average()
                .orElse(0);

        Menu menu = salesList.get(0).getMenu();
        int originalExpected = menu.getExpectedDailySales();
        int corrected        = (int) Math.round(avgActualDailySales);
        double correctionRate = originalExpected > 0
                ? Math.round(avgActualDailySales / originalExpected * 1000.0) / 1000.0
                : 0;

        return SalesCorrectionResponseDto.builder()
                .success(true)
                .data(SalesCorrectionResponseDto.DataDto.builder()
                        .menuId(menuId)
                        .originalExpectedDailySales(originalExpected)
                        .correctedExpectedDailySales(corrected)
                        .correctionRate(correctionRate)
                        .basedOnDays(salesList.size())
                        .lastSalesDate(salesList.get(salesList.size() - 1).getSalesDate())
                        .build())
                .build();
    }
}
