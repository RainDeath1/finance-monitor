package com.globus.finance.controller;

import com.globus.finance.dto.CategoryReportRow;
import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportDetailRow;
import com.globus.finance.dto.ReportPeriod;
import com.globus.finance.entity.Transaction;
import com.globus.finance.service.ReportService;
import com.globus.finance.service.export.XmlReportExportService;
import com.globus.finance.service.export.ExcelReportExportService;
import com.globus.finance.service.export.PdfReportExportService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ReportExportController {

    private final ReportService reportService;
    private final XmlReportExportService xmlExportService;
    private final ExcelReportExportService excelExportService;

    private final PdfReportExportService pdfExportService;

    public ReportExportController(
            ReportService reportService,
            XmlReportExportService xmlExportService,
            ExcelReportExportService excelExportService,
            PdfReportExportService pdfExportService) {

        this.pdfExportService = pdfExportService;
        this.reportService = reportService;
        this.xmlExportService = xmlExportService;
        this.excelExportService = excelExportService;
    }
    @GetMapping({"/reports/export", "/reports/export/xml"})
    public ResponseEntity<byte[]> exportReport(

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

            @RequestParam(name = "format", defaultValue = "xml")
            String format,

            @RequestParam(name = "detail", defaultValue = "transactions")
            String detail) {

        try {
            Long userId = 1L;

            LocalDate today = LocalDate.now();

            int selectedYear =
                    year != null ? year : today.getYear();

            int selectedMonth =
                    month != null ? month : today.getMonthValue();

            int selectedQuarter =
                    quarter != null
                            ? quarter
                            : (today.getMonthValue() - 1) / 3 + 1;

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

            FinancialMetrics summary =
                    reportService.getSummary(
                            userId,
                            reportPeriod
                    );

            List<Transaction> transactions = List.of();

            List<ReportDetailRow> groupedDetails = List.of();

            if ("transactions".equals(detail)) {

                transactions =
                        reportService.getTransactions(
                                userId,
                                reportPeriod
                        );

            } else if ("days".equals(detail)
                    || "months".equals(detail)) {

                groupedDetails =
                        reportService.getGroupedDetails(
                                userId,
                                reportPeriod,
                                detail
                        );

            } else {
                throw new IllegalArgumentException(
                        "Некорректный тип детализации"
                );
            }

            List<CategoryReportRow> categories =
                    reportService.getCategoryDetails(
                            userId,
                            reportPeriod
                    );

            byte[] file;
            String filename;
            MediaType contentType;

            switch (format) {

                case "xml" -> {

                    file = xmlExportService.export(
                            reportPeriod,
                            summary,
                            detail,
                            transactions,
                            groupedDetails,
                            categories
                    );

                    filename = "finance-report.xml";

                    contentType = MediaType.parseMediaType(
                            "application/xml;charset=UTF-8"
                    );
                }

                case "xlsx" -> {

                    file = excelExportService.export(
                            reportPeriod,
                            summary,
                            detail,
                            transactions,
                            groupedDetails,
                            categories
                    );

                    filename = "finance-report.xlsx";

                    contentType = MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    );
                }

                case "pdf" -> {

                    file = pdfExportService.export(
                            reportPeriod,
                            summary,
                            detail,
                            transactions,
                            groupedDetails,
                            categories
                    );

                    filename = "finance-report.pdf";

                    contentType = MediaType.APPLICATION_PDF;
                }

                default -> throw new IllegalArgumentException(
                        "Неподдерживаемый формат отчёта: " + format
                );
            }

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\""
                    )
                    .contentType(contentType)
                    .body(file);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    e
            );
        }
    }
}