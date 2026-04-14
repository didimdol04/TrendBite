package com.trendbite.menuprofit.service;

import com.trendbite.menuprofit.dto.request.FinanceRequestDto;
import com.trendbite.menuprofit.dto.request.sub.EmployeeDto;
import com.trendbite.menuprofit.dto.response.FinanceResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinanceService {

    private static final double WEEKS_PER_MONTH                  = 4.345;
    private static final double NIGHT_SHIFT_EXTRA                = 0.5;
    private static final double MIN_HOURS_FOR_HOLIDAY_PAY        = 15.0;
    private static final double DEFAULT_DAILY_WORK_HOURS         = 8.0;
    private static final double WEEKLY_HOLIDAY_PAY_BASE_HOURS    = 8.0; // 주휴수당 기준 하루 소정근로시간 (근로기준법 기준)

    public FinanceResponseDto calculate(FinanceRequestDto req) {

        // 1. 단위당 직접재료원가
        int ingredientCost        = req.getDirectMaterialCost().getIngredientCostPerUnit();
        int packagingCost         = req.getMenuInfo().getPackagingCostPerUnit();
        int directMaterialPerUnit = ingredientCost + packagingCost;

        // 2. 월 전체 인건비
        long totalMonthlyLaborCost = calcTotalMonthlyLaborCost(
                req.getDirectLaborCost().getEmployees());

        // 3. 단위당 직접노무원가
        int    workingDaysPerWeek     = req.getSalesAssumption().getWorkingDaysPerWeek();
        double monthlyWorkingDays     = workingDaysPerWeek * WEEKS_PER_MONTH;
        long   monthlyWorkingDaysLong = Math.round(monthlyWorkingDays); // 반올림 후 long 변환 (truncation 방지)

        // 일 영업시간: 사용자 입력값 우선, 없으면 기본값 8.0
        double dailyWorkHours = req.getSalesAssumption().getDailyWorkHours() != null
                ? req.getSalesAssumption().getDailyWorkHours()
                : DEFAULT_DAILY_WORK_HOURS;

        double hourlyLaborRate = totalMonthlyLaborCost / (monthlyWorkingDays * dailyWorkHours);

        double totalActivityHours = (req.getDirectLaborCost().getBohTimeMinutes()
                + req.getDirectLaborCost().getFohTimeMinutes()) / 60.0;
        int directLaborPerUnit = (int) Math.round(hourlyLaborRate * totalActivityHours);

        // 4. 단위당 변동제조간접원가
        int variableOverheadPerUnit = req.getVariableOverheadCostPerUnit();

        // 5. 단위당 총 변동원가 & 공헌이익
        int totalVariablePerUnit = directMaterialPerUnit + directLaborPerUnit + variableOverheadPerUnit;
        int sellingPrice         = req.getMenuInfo().getSellingPrice();
        int contributionMargin   = sellingPrice - totalVariablePerUnit;

        // 6. 월 매출 & 변동원가
        int  expectedDailySales  = req.getSalesAssumption().getExpectedDailySales();
        long monthlyRevenue      = (long) sellingPrice * expectedDailySales * monthlyWorkingDaysLong;
        long monthlyVariableCost = (long) totalVariablePerUnit * expectedDailySales * monthlyWorkingDaysLong;

        // 7. 고정비 합산
        long fixedCostTotal = calcFixedCost(req);

        // 8. 영업이익
        long operatingProfit = monthlyRevenue - monthlyVariableCost - fixedCostTotal;

        // 9. 이익률 & 원가율
        double profitMargin = monthlyRevenue > 0
                ? Math.round((double) operatingProfit / monthlyRevenue * 1000.0) / 1000.0 : 0;
        double foodCostRate = monthlyRevenue > 0
                ? Math.round((double) (ingredientCost * expectedDailySales * monthlyWorkingDaysLong)
                / monthlyRevenue * 1000.0) / 1000.0 : 0;

        // 10. BEP (고정비 배분 적용)
        long allocatedFixedCost = calcAllocatedFixedCost(fixedCostTotal, monthlyRevenue, req);
        int bepPerDay = contributionMargin > 0
                ? (int) Math.ceil((double) allocatedFixedCost / contributionMargin / monthlyWorkingDays)
                : Integer.MAX_VALUE;
        long bepRevenue = (long) bepPerDay * sellingPrice * monthlyWorkingDaysLong;

        // 11. 증분 이익 (baselineProfit 있을 때만)
        FinanceResponseDto.IncrementalDto incremental = null;
        if (req.getBaselineProfit() != null) {
            long baselineProfit = req.getBaselineProfit().getCurrentMonthlyOperatingProfit();
            incremental = FinanceResponseDto.IncrementalDto.builder()
                    .additionalRevenue(monthlyRevenue)
                    .additionalOperatingProfit(operatingProfit)
                    .profitGrowthRate(baselineProfit > 0
                            ? Math.round((double) operatingProfit / baselineProfit * 1000.0) / 1000.0
                            : 0)
                    .build();
        }

        return FinanceResponseDto.builder()
                .success(true)
                .data(FinanceResponseDto.DataDto.builder()
                        .unitEconomics(FinanceResponseDto.UnitEconomicsDto.builder()
                                .sellingPrice(sellingPrice)
                                .directMaterialCostPerUnit(directMaterialPerUnit)
                                .directLaborCostPerUnit(directLaborPerUnit)
                                .variableOverheadPerUnit(variableOverheadPerUnit)
                                .totalVariableCostPerUnit(totalVariablePerUnit)
                                .contributionMarginPerUnit(contributionMargin)
                                .build())
                        .monthlyResult(FinanceResponseDto.MonthlyResultDto.builder()
                                .totalRevenue(monthlyRevenue)
                                .totalVariableCost(monthlyVariableCost)
                                .fixedCostAllocation(fixedCostTotal)
                                .operatingProfit(operatingProfit)
                                .profitMargin(profitMargin)
                                .foodCostRate(foodCostRate)
                                .build())
                        .breakEven(FinanceResponseDto.BreakEvenDto.builder()
                                .breakEvenQuantityPerDay(bepPerDay)
                                .breakEvenRevenuePerMonth(bepRevenue)
                                .build())
                        .incremental(incremental)
                        .build())
                .build();
    }

    // ── private 메서드 ──────────────────────────────────

    private long calcTotalMonthlyLaborCost(List<EmployeeDto> employees) {
        if (employees == null || employees.isEmpty()) return 0L;
        long total = 0L;
        for (EmployeeDto emp : employees) {
            if ("simple".equalsIgnoreCase(emp.getMode())) {
                total += emp.getMonthlyTotal();
            } else {
                total += calcDetailMonthlyLaborCost(emp);
            }
        }
        return total;
    }

    private long calcDetailMonthlyLaborCost(EmployeeDto emp) {
        double hourlyWage  = emp.getHourlyWage();
        double weeklyHours = emp.getWeeklyHours();

        double basicMonthly = hourlyWage * weeklyHours * WEEKS_PER_MONTH;

        // 주휴수당: 주 15시간 이상에만 적용
        double weeklyHolidayPay = 0;
        if (weeklyHours >= MIN_HOURS_FOR_HOLIDAY_PAY) {
            weeklyHolidayPay = hourlyWage * WEEKLY_HOLIDAY_PAY_BASE_HOURS
                    * (weeklyHours / 40.0) * WEEKS_PER_MONTH;
        }

        // 야간수당 (1.5배)
        double nightPay = 0;
        if (Boolean.TRUE.equals(emp.getHasNightShift())
                && emp.getNightShiftHoursPerWeek() != null) {
            nightPay = hourlyWage * NIGHT_SHIFT_EXTRA
                    * emp.getNightShiftHoursPerWeek() * WEEKS_PER_MONTH;
        }

        // TODO: 5인 이상 사업장 연장근로수당 추가
        return Math.round(basicMonthly + weeklyHolidayPay + nightPay);
    }

    private long calcAllocatedFixedCost(long fixedCostTotal,
                                        long menuMonthlyRevenue,
                                        FinanceRequestDto req) {
        if (req.getBaselineProfit() == null) {
            // 신규 창업자: 전체 고정비 그대로
            return fixedCostTotal;
        }
        Long totalRevenue = req.getBaselineProfit().getCurrentMonthlyRevenue();
        if (totalRevenue == null || totalRevenue <= 0) {
            return fixedCostTotal;
        }
        // 신메뉴 매출 비율만큼 고정비 배분
        double ratio = Math.min((double) menuMonthlyRevenue / totalRevenue, 1.0);
        return Math.round(fixedCostTotal * ratio);
    }

    private long calcFixedCost(FinanceRequestDto req) {
        var fc = req.getFixedCost();
        long total = 0L;
        if (fc.getMonthlyRent()   != null) total += fc.getMonthlyRent();
        if (fc.getManagementFee() != null) total += fc.getManagementFee();
        if (fc.getMarketingCost() != null) total += fc.getMarketingCost();
        if (fc.getMiscCost()      != null) total += fc.getMiscCost();
        return total;
    }
}
