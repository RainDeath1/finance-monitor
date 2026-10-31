package com.globus.finance.service.export;

import com.globus.finance.dto.CategoryReportRow;
import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportDetailRow;
import com.globus.finance.dto.ReportPeriod;
import com.globus.finance.entity.Transaction;
import com.globus.finance.entity.TransactionType;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ExcelReportExportService {

    public byte[] export(
            ReportPeriod period,
            FinancialMetrics summary,
            String detail,
            List<Transaction> transactions,
            List<ReportDetailRow> groupedDetails,
            List<CategoryReportRow> categories) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);

            CellStyle moneyStyle = workbook.createCellStyle();

            DataFormat dataFormat = workbook.createDataFormat();

            moneyStyle.setDataFormat(
                    dataFormat.getFormat("#,##0.00 \"₸\"")
            );

            createSummarySheet(
                    workbook,
                    period,
                    summary,
                    headerStyle,
                    moneyStyle
            );

            createDetailsSheet(
                    workbook,
                    detail,
                    transactions,
                    groupedDetails,
                    headerStyle,
                    moneyStyle
            );

            createCategoriesSheet(
                    workbook,
                    categories,
                    headerStyle,
                    moneyStyle
            );

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Ошибка формирования Excel-отчёта", e
            );
        }
    }

    private void createSummarySheet(
            Workbook workbook,
            ReportPeriod period,
            FinancialMetrics summary,
            CellStyle headerStyle,
            CellStyle moneyStyle) {

        Sheet sheet = workbook.createSheet("Сводка");

        Row title = sheet.createRow(0);
        text(title, 0, "Финансовый отчёт");
        title.getCell(0).setCellStyle(headerStyle);

        Row periodRow = sheet.createRow(2);

        text(periodRow, 0, "Период");
        text(periodRow, 1, period.title());

        Row startRow = sheet.createRow(3);

        text(startRow, 0, "Дата начала");
        text(startRow, 1, period.startDate().toString());

        Row endRow = sheet.createRow(4);

        text(endRow, 0, "Дата окончания");
        text(
                endRow,
                1,
                period.endDate().minusDays(1).toString()
        );

        Row incomeRow = sheet.createRow(6);

        text(incomeRow, 0, "Доходы");
        money(incomeRow, 1, summary.income(), moneyStyle);

        Row expensesRow = sheet.createRow(7);

        text(expensesRow, 0, "Расходы");
        money(expensesRow, 1, summary.expenses(), moneyStyle);

        Row balanceRow = sheet.createRow(8);

        text(balanceRow, 0, "Баланс");
        money(balanceRow, 1, summary.balance(), moneyStyle);

        sheet.setColumnWidth(0, 25 * 256);
        sheet.setColumnWidth(1, 40 * 256);
    }

    private void createDetailsSheet(
            Workbook workbook,
            String detail,
            List<Transaction> transactions,
            List<ReportDetailRow> groupedDetails,
            CellStyle headerStyle,
            CellStyle moneyStyle) {

        Sheet sheet = workbook.createSheet("Детализация");

        int rowIndex = 1;

        if ("transactions".equals(detail)) {

            header(
                    sheet,
                    headerStyle,
                    "Дата",
                    "Категория",
                    "Тип",
                    "Сумма",
                    "Описание"
            );

            for (Transaction transaction : transactions) {

                Row row = sheet.createRow(rowIndex++);

                text(
                        row,
                        0,
                        transaction.getTransactionDate().toString()
                );

                text(
                        row,
                        1,
                        transaction.getCategory().getName()
                );

                text(
                        row,
                        2,
                        transaction.getType() == TransactionType.INCOME
                                ? "Доход"
                                : "Расход"
                );

                money(
                        row,
                        3,
                        transaction.getAmount(),
                        moneyStyle
                );

                text(
                        row,
                        4,
                        transaction.getDescription()
                );
            }

        } else if ("days".equals(detail)
                || "months".equals(detail)) {

            header(
                    sheet,
                    headerStyle,
                    "Период",
                    "Доходы",
                    "Расходы",
                    "Баланс"
            );

            for (ReportDetailRow item : groupedDetails) {

                Row row = sheet.createRow(rowIndex++);

                text(row, 0, item.label());

                money(
                        row,
                        1,
                        item.income(),
                        moneyStyle
                );

                money(
                        row,
                        2,
                        item.expenses(),
                        moneyStyle
                );

                money(
                        row,
                        3,
                        item.balance(),
                        moneyStyle
                );
            }

        } else {
            throw new IllegalArgumentException(
                    "Неизвестный тип детализации"
            );
        }

        sheet.createFreezePane(0, 1);

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 22 * 256);
        sheet.setColumnWidth(4, 55 * 256);
    }

    private void createCategoriesSheet(
            Workbook workbook,
            List<CategoryReportRow> categories,
            CellStyle headerStyle,
            CellStyle moneyStyle) {

        Sheet sheet = workbook.createSheet("Категории");

        header(
                sheet,
                headerStyle,
                "Категория",
                "Тип",
                "Сумма"
        );

        int rowIndex = 1;

        for (CategoryReportRow category : categories) {

            Row row = sheet.createRow(rowIndex++);

            text(row, 0, category.category());

            text(
                    row,
                    1,
                    category.type() == TransactionType.INCOME
                            ? "Доход"
                            : "Расход"
            );

            money(
                    row,
                    2,
                    category.amount(),
                    moneyStyle
            );
        }

        sheet.createFreezePane(0, 1);

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 25 * 256);
    }

    private void header(
            Sheet sheet,
            CellStyle style,
            String... columns) {

        Row row = sheet.createRow(0);

        for (int i = 0; i < columns.length; i++) {

            Cell cell = row.createCell(i);

            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    private void text(
            Row row,
            int column,
            String value) {

        Cell cell = row.createCell(column);

        cell.setCellValue(value == null ? "" : value);
    }

    private void money(
            Row row,
            int column,
            BigDecimal value,
            CellStyle style) {

        Cell cell = row.createCell(column);

        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }
}