
package com.globus.finance.controller;

import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.service.AnalyticsService;
import com.globus.finance.dto.MonthlyMetrics;
import com.globus.finance.dto.DailyMetrics;

import java.math.BigDecimal;
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
            @RequestParam(name = "period", defaultValue = "year") String period,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
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

        List<String> monthNames = List.of(
                "Январь",
                "Февраль",
                "Март",
                "Апрель",
                "Май",
                "Июнь",
                "Июль",
                "Август",
                "Сентябрь",
                "Октябрь",
                "Ноябрь",
                "Декабрь"
        );

        model.addAttribute("monthNames", monthNames);

        List<String> chartLabels;
        List<BigDecimal> chartIncomes;
        List<BigDecimal> chartExpenses;

        if ("month".equals(period)) {

            // Получаем ежедневную статистику
            List<DailyMetrics> dailyMetrics =
                    analyticsService.getDailyMetrics(
                            userId,
                            selectedYear,
                            selectedMonth
                    );

            // Названия дней: 1, 2, 3 ... 31
            chartLabels = dailyMetrics.stream()
                    .map(day -> String.valueOf(
                            day.date().getDayOfMonth()))
                    .toList();

            chartIncomes = dailyMetrics.stream()
                    .map(DailyMetrics::income)
                    .toList();

            chartExpenses = dailyMetrics.stream()
                    .map(DailyMetrics::expenses)
                    .toList();

        } else {

            // Для квартала и года используем месячную статистику
            List<MonthlyMetrics> monthlyMetrics =
                    analyticsService.getMonthlyMetrics(
                            userId,
                            selectedYear
                    );

            int firstMonth = startDate.getMonthValue();

            int monthsCount = "quarter".equals(period) ? 3 : 12;

            List<MonthlyMetrics> chartMetrics =
                    monthlyMetrics.subList(
                            firstMonth - 1,
                            firstMonth - 1 + monthsCount
                    );

            chartLabels = monthNames.subList(
                    firstMonth - 1,
                    firstMonth - 1 + monthsCount
            );

            chartIncomes = chartMetrics.stream()
                    .map(MonthlyMetrics::income)
                    .toList();

            chartExpenses = chartMetrics.stream()
                    .map(MonthlyMetrics::expenses)
                    .toList();
        }

// Передаём данные в Thymeleaf
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("monthlyIncomes", chartIncomes);
        model.addAttribute("monthlyExpenses", chartExpenses);


        String chartTitle = switch (period) {
            case "month" ->
                    "Доходы и расходы за " +
                            monthNames.get(selectedMonth - 1).toLowerCase() +
                            " " + selectedYear + " года";

            case "quarter" ->
                    "Доходы и расходы за " +
                            selectedQuarter + " квартал " +
                            selectedYear;

            default ->
                    "Динамика доходов и расходов за " +
                            selectedYear + " год";
        };

        model.addAttribute("chartTitle", chartTitle);

        model.addAttribute("year", selectedYear);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("quarter", selectedQuarter);
        model.addAttribute("period", period);

        return "dashboard";
    }
}
