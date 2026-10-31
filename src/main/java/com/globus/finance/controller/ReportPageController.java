package com.globus.finance.controller;
import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportPeriod;
import com.globus.finance.service.ReportService;
import com.globus.finance.entity.Transaction;
import com.globus.finance.dto.ReportDetailRow;
import com.globus.finance.dto.CategoryReportRow;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ReportPageController {

    private final ReportService reportService;

    public ReportPageController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(name = "period", defaultValue = "month")
            String period,

            @RequestParam(name = "year", required = false)
            Integer year,

            @RequestParam(name = "month", required = false)
            Integer month,

            @RequestParam(name = "quarter", required = false)
            Integer quarter,

            @RequestParam(name = "day", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate day,

            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(name = "detail", defaultValue = "transactions")
            String detail,

            @RequestParam(name = "generate", defaultValue = "false")
            boolean generate, Model model) {

        LocalDate today = LocalDate.now();

        model.addAttribute("detail", detail);

        int selectedYear =
                year != null ? year : today.getYear();

        int selectedMonth =
                month != null ? month : today.getMonthValue();

        int selectedQuarter =
                quarter != null
                        ? quarter
                        : (today.getMonthValue() - 1) / 3 + 1;

        model.addAttribute("period", period);
        model.addAttribute("year", selectedYear);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("quarter", selectedQuarter);
        model.addAttribute("day", day != null ? day : today);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        model.addAttribute("monthNames", List.of(
                "Январь", "Февраль", "Март",
                "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь",
                "Октябрь", "Ноябрь", "Декабрь"
        ));

        if (generate) {

            try {
                Long userId = 1L;

                ReportPeriod reportPeriod =
                        reportService.buildPeriod(
                                period,
                                selectedYear,
                                selectedMonth,
                                selectedQuarter,
                                day,
                                dateFrom,
                                dateTo
                        );

                FinancialMetrics metrics =
                        reportService.getSummary(
                                userId,
                                reportPeriod
                        );

                model.addAttribute("report", reportPeriod);
                model.addAttribute("summary", metrics);

                switch (detail) {

                    case "transactions" -> {

                        List<Transaction> transactions =
                                reportService.getTransactions(
                                        userId,
                                        reportPeriod
                                );

                        model.addAttribute(
                                "transactions",
                                transactions
                        );

                    }

                    case "days", "months" -> {

                        List<ReportDetailRow> groupedDetails =
                                reportService.getGroupedDetails(
                                        userId,
                                        reportPeriod,
                                        detail
                                );

                        model.addAttribute(
                                "groupedDetails",
                                groupedDetails
                        );
                    }

                    default -> throw new IllegalArgumentException(
                            "Неизвестный тип детализации отчёта"
                    );
                }

                List<CategoryReportRow> categoryDetails =
                        reportService.getCategoryDetails(
                                userId,
                                reportPeriod
                        );

                model.addAttribute("categoryDetails", categoryDetails);

            } catch (IllegalArgumentException e) {

                model.addAttribute(
                        "error",
                        e.getMessage()
                );
            }
        }

        return "reports";
    }
}