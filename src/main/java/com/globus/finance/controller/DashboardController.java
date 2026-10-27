
package com.globus.finance.controller;

import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.service.AnalyticsService;
import com.globus.finance.dto.MonthlyMetrics;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private final AnalyticsService analyticsService;

    public DashboardController(
            AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(defaultValue = "year") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            Model model) {

        Long userId = 1L;

        LocalDate today = LocalDate.now();

        int selectedYear = year != null
                ? year
                : today.getYear();

        int selectedMonth = month != null
                ? month
                : today.getMonthValue();

        int selectedQuarter = quarter != null
                ? quarter
                : (today.getMonthValue() - 1) / 3 + 1;

        if (selectedYear < 1900 || selectedYear > 2100) {
            throw new IllegalArgumentException("Некорректный год");
        }

        LocalDate startDate;
        LocalDate endDate;

        switch (period) {

            case "month" -> {

                if (selectedMonth < 1 || selectedMonth > 12) {
                    throw new IllegalArgumentException(
                            "Некорректный месяц");
                }

                startDate = LocalDate.of(
                        selectedYear, selectedMonth, 1);

                endDate = startDate.plusMonths(1);
            }

            case "quarter" -> {

                if (selectedQuarter < 1 || selectedQuarter > 4) {
                    throw new IllegalArgumentException(
                            "Некорректный квартал");
                }

                int firstMonth = (selectedQuarter - 1) * 3 + 1;

                startDate = LocalDate.of(
                        selectedYear, firstMonth, 1);

                endDate = startDate.plusMonths(3);
            }

            case "year" -> {

                startDate = LocalDate.of(selectedYear, 1, 1);

                endDate = startDate.plusYears(1);
            }

            default -> throw new IllegalArgumentException(
                    "Неизвестный тип периода");
        }

        FinancialMetrics metrics =
                analyticsService.getMetricsForPeriod(
                        userId,
                        startDate,
                        endDate
                );

        model.addAttribute("income", metrics.income());
        model.addAttribute("expenses", metrics.expenses());
        model.addAttribute("balance", metrics.balance());

        List<MonthlyMetrics> monthlyMetrics =
                analyticsService.getMonthlyMetrics(
                        userId,
                        selectedYear
                );

        model.addAttribute(
                "monthlyIncomes",
                monthlyMetrics.stream()
                        .map(MonthlyMetrics::income)
                        .toList()
        );

        model.addAttribute(
                "monthlyExpenses",
                monthlyMetrics.stream()
                        .map(MonthlyMetrics::expenses)
                        .toList()
        );

        model.addAttribute("year", selectedYear);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("quarter", selectedQuarter);
        model.addAttribute("period", period);

        return "dashboard";
    }
}
