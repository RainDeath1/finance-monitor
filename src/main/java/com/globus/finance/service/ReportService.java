package com.globus.finance.service;

import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportPeriod;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ReportService {

    private final AnalyticsService analyticsService;

    public ReportService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public ReportPeriod buildPeriod(
            String period,
            Integer year,
            Integer month,
            Integer quarter,
            LocalDate day,
            LocalDate dateFrom,
            LocalDate dateTo) {

        LocalDate today = LocalDate.now();

        String selectedPeriod =
                period == null || period.isBlank()
                        ? "month"
                        : period;

        int selectedYear =
                year != null ? year : today.getYear();

        int selectedMonth =
                month != null ? month : today.getMonthValue();

        int selectedQuarter =
                quarter != null
                        ? quarter
                        : (today.getMonthValue() - 1) / 3 + 1;

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return switch (selectedPeriod) {

            case "day" -> {

                LocalDate selectedDay =
                        day != null ? day : today;

                yield new ReportPeriod(
                        selectedDay,
                        selectedDay.plusDays(1),
                        "Отчёт за " + selectedDay.format(formatter)
                );
            }

            case "month" -> {

                LocalDate start =
                        LocalDate.of(selectedYear, selectedMonth, 1);

                yield new ReportPeriod(
                        start,
                        start.plusMonths(1),
                        "Отчёт за " + start.getMonthValue()
                                + "." + selectedYear
                );
            }

            case "quarter" -> {

                if (selectedQuarter < 1 || selectedQuarter > 4) {
                    throw new IllegalArgumentException(
                            "Некорректный квартал");
                }

                int firstMonth =
                        (selectedQuarter - 1) * 3 + 1;

                LocalDate start =
                        LocalDate.of(selectedYear, firstMonth, 1);

                yield new ReportPeriod(
                        start,
                        start.plusMonths(3),
                        "Отчёт за " + selectedQuarter
                                + " квартал " + selectedYear
                );
            }

            case "year" -> {

                LocalDate start =
                        LocalDate.of(selectedYear, 1, 1);

                yield new ReportPeriod(
                        start,
                        start.plusYears(1),
                        "Отчёт за " + selectedYear + " год"
                );
            }

            case "custom" -> {

                if (dateFrom == null || dateTo == null
                        || dateFrom.isAfter(dateTo)) {

                    throw new IllegalArgumentException(
                            "Некорректный диапазон дат");
                }

                yield new ReportPeriod(
                        dateFrom,
                        dateTo.plusDays(1),
                        "Отчёт с " + dateFrom.format(formatter)
                                + " по " + dateTo.format(formatter)
                );
            }

            default -> throw new IllegalArgumentException(
                    "Неизвестный тип отчётного периода");
        };
    }

    public FinancialMetrics getSummary(
            Long userId,
            ReportPeriod period) {

        return analyticsService.getMetricsForPeriod(
                userId,
                period.startDate(),
                period.endDate()
        );
    }
}