package com.globus.finance.service;

import com.globus.finance.dto.CategoryReportRow;
import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportPeriod;
import com.globus.finance.entity.Transaction;
import com.globus.finance.repository.TransactionRepository;
import com.globus.finance.dto.ReportDetailRow;
import com.globus.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;


import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Service
public class ReportService {

    private final AnalyticsService analyticsService;
    private final TransactionRepository transactionRepository;

    public ReportService(
            AnalyticsService analyticsService,
            TransactionRepository transactionRepository) {

        this.analyticsService = analyticsService;
        this.transactionRepository = transactionRepository;
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

    public List<Transaction> getTransactions(
            Long userId,
            ReportPeriod period) {

        if (userId == null || period == null
                || period.startDate() == null
                || period.endDate() == null
                || !period.startDate().isBefore(period.endDate())) {

            throw new IllegalArgumentException(
                    "Некорректные параметры отчёта");
        }

        return transactionRepository.findForReport(
                userId,
                period.startDate(),
                period.endDate()
        );
    }

    public List<ReportDetailRow> getGroupedDetails(
            Long userId,
            ReportPeriod period,
            String detail) {

        if (!"days".equals(detail) && !"months".equals(detail)) {
            throw new IllegalArgumentException(
                    "Неизвестный тип детализации");
        }

        List<Transaction> transactions =
                getTransactions(userId, period);


        Map<LocalDate, List<Transaction>> grouped =
                transactions.stream()
                        .collect(Collectors.groupingBy(
                                transaction -> {
                                    LocalDate date =
                                            transaction.getTransactionDate();

                                    return "months".equals(detail)
                                            ? date.withDayOfMonth(1)
                                            : date;
                                },
                                TreeMap::new,
                                Collectors.toList()
                        ));

        if ("months".equals(detail)) {

            LocalDate currentMonth =
                    period.startDate().withDayOfMonth(1);

            while (currentMonth.isBefore(period.endDate())) {

                grouped.putIfAbsent(currentMonth, List.of());

                currentMonth = currentMonth.plusMonths(1);
            }
        }

        DateTimeFormatter formatter = "months".equals(detail)
                ? DateTimeFormatter.ofPattern("MM.yyyy")
                : DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return grouped.entrySet().stream()
                .map(entry -> {

                    BigDecimal income = entry.getValue().stream()
                            .filter(t -> t.getType() == TransactionType.INCOME)
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expenses = entry.getValue().stream()
                            .filter(t -> t.getType() == TransactionType.EXPENSE)
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new ReportDetailRow(
                            entry.getKey().format(formatter),
                            income,
                            expenses,
                            income.subtract(expenses)
                    );
                })
                .toList();
    }

    public List<CategoryReportRow> getCategoryDetails(
            Long userId,
            ReportPeriod period) {

        List<Transaction> transactions =
                getTransactions(userId, period);

        Map<Long, List<Transaction>> grouped =
                transactions.stream()
                        .collect(Collectors.groupingBy(
                                t -> t.getCategory().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        return grouped.values().stream()
                .map(categoryTransactions -> {

                    Transaction first =
                            categoryTransactions.get(0);

                    BigDecimal total =
                            categoryTransactions.stream()
                                    .map(Transaction::getAmount)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    );

                    return new CategoryReportRow(
                            first.getCategory().getName(),
                            first.getType(),
                            total
                    );
                })
                .toList();
    }
}