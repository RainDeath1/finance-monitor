package com.globus.finance.service.export;

import com.globus.finance.dto.CategoryReportRow;
import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportDetailRow;
import com.globus.finance.dto.ReportPeriod;
import com.globus.finance.entity.Transaction;
import com.globus.finance.entity.TransactionType;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;


import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PdfReportExportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] export(
            ReportPeriod period,
            FinancialMetrics summary,
            String detail,
            List<Transaction> transactions,
            List<ReportDetailRow> groupedDetails,
            List<CategoryReportRow> categories) {

        try {
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] fontBytes;

            ClassPathResource fontResource =
                    new ClassPathResource("fonts/ttf/DejaVuSans.ttf");

            try (InputStream input = fontResource.getInputStream()) {
                fontBytes = input.readAllBytes();
            }

            PdfDocument pdf = new PdfDocument(
                    new PdfWriter(output)
            );

            pdf.setDefaultPageSize(PageSize.A4.rotate());

            try (Document document = new Document(
                    pdf,
                    PageSize.A4.rotate(),
                    false)) {

                PdfFont font = PdfFontFactory.createFont(
                        fontBytes,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
                );

                document.setFont(font);
                document.setFontSize(10);
                document.setMargins(30, 30, 40, 30);

                document.add(
                        new Paragraph("FINANCE MONITOR")
                                .setFontSize(22)
                                .setFontColor(new DeviceRgb(37, 99, 235))
                                .setMarginBottom(5)
                );

                document.add(
                        new Paragraph(period.title())
                                .setFontSize(16)
                                .setMarginBottom(10)
                );

                document.add(
                        new Paragraph(
                                "Период: "
                                        + period.startDate().format(DATE_FORMAT)
                                        + " — "
                                        + period.endDate()
                                        .minusDays(1)
                                        .format(DATE_FORMAT)
                        )
                );

                document.add(
                        new Paragraph("Валюта: KZT (₸)")
                );

                document.add(
                        new Paragraph(
                                "Дата формирования: "
                                        + LocalDateTime.now()
                                        .format(DATETIME_FORMAT)
                        )
                                .setFontSize(9)
                                .setFontColor(ColorConstants.GRAY)
                                .setMarginBottom(15)
                );

                document.add(
                        new Paragraph("Финансовая сводка")
                                .setFontSize(12)
                );

                Table summaryTable = createTable(
                        new float[]{1, 1, 1},
                        "Доходы",
                        "Расходы",
                        "Баланс"
                );

                summaryTable.addCell(
                        amountCell(summary.income(), 0)
                );

                summaryTable.addCell(
                        amountCell(summary.expenses(), 0)
                );

                summaryTable.addCell(
                        amountCell(summary.balance(), 0)
                );

                document.add(summaryTable);

                document.add(
                        new Paragraph("Детализация отчёта")
                                .setFontSize(12)
                                .setMarginTop(20)
                );

                if ("transactions".equals(detail)) {

                    Table transactionTable = createTable(
                            new float[]{2, 2, 1, 2, 4},
                            "Дата",
                            "Категория",
                            "Тип",
                            "Сумма",
                            "Описание"
                    );

                        for (Transaction transaction : transactions) {

                            transactionTable.addCell(
                                    transaction.getTransactionDate().toString()
                            );

                            transactionTable.addCell(
                                    transaction.getCategory().getName()
                            );

                            transactionTable.addCell(
                                    transaction.getType() == TransactionType.INCOME
                                            ? "Доход"
                                            : "Расход"
                            );

                            transactionTable.addCell(
                                    money(transaction.getAmount())
                            );

                            transactionTable.addCell(
                                    transaction.getDescription() == null
                                            ? ""
                                            : transaction.getDescription()
                            );
                        }

                        document.add(transactionTable);

                    int totalPages = pdf.getNumberOfPages();

                    for (int page = 1; page <= totalPages; page++) {

                        document.showTextAligned(
                                new Paragraph(
                                        "Страница " + page + " из " + totalPages
                                )
                                        .setFont(font)
                                        .setFontSize(8)
                                        .setFontColor(ColorConstants.GRAY),

                                PageSize.A4.rotate().getWidth() / 2,
                                20,
                                page,
                                TextAlignment.CENTER,
                                VerticalAlignment.MIDDLE,
                                0
                        );
                    }

                    } else if ("days".equals(detail)
                        || "months".equals(detail)) {

                    Table groupedTable = createTable(
                            new float[]{2, 2, 2, 2},
                            "Период",
                            "Доходы",
                            "Расходы",
                            "Баланс"
                    );

                    int rowIndex = 0;

                    for (ReportDetailRow row : groupedDetails) {

                        groupedTable.addCell(
                                dataCell(row.label(), rowIndex, false)
                        );

                        groupedTable.addCell(
                                amountCell(row.income(), rowIndex)
                        );

                        groupedTable.addCell(
                                amountCell(row.expenses(), rowIndex)
                        );

                        groupedTable.addCell(
                                amountCell(row.balance(), rowIndex)
                        );

                        rowIndex++;
                    }

                    document.add(groupedTable);

                } else {
                    throw new IllegalArgumentException(
                            "Неизвестный тип детализации"
                    );
                }

                document.add(
                        new Paragraph("Доходы и расходы по категориям")
                                .setFontSize(12)
                                .setMarginTop(20)
                );

                Table categoriesTable = createTable(
                        new float[]{3, 2, 2},
                        "Категория",
                        "Тип",
                        "Сумма"
                );

                int categoryRow = 0;

                for (CategoryReportRow category : categories) {

                    categoriesTable.addCell(
                            dataCell(
                                    category.category(),
                                    categoryRow,
                                    false
                            )
                    );

                    categoriesTable.addCell(
                            dataCell(
                                    category.type() == TransactionType.INCOME
                                            ? "Доход"
                                            : "Расход",
                                    categoryRow,
                                    false
                            )
                    );

                    categoriesTable.addCell(
                            amountCell(
                                    category.amount(),
                                    categoryRow
                            )
                    );

                    categoryRow++;
                }

                document.add(categoriesTable);
            }

            return output.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Ошибка формирования PDF-отчёта",
                    e
            );
        }
    }

    private Table createTable(
            float[] widths,
            String... headers) {

        Table table = new Table(
                UnitValue.createPercentArray(widths)
        ).useAllAvailableWidth();

        // Цвет заголовков
        DeviceRgb headerColor =
                new DeviceRgb(37, 99, 235);

        for (String header : headers) {

            Cell cell = new Cell()
                    .add(
                            new Paragraph(header)
                                    .setMargin(0)
                    )
                    .setBackgroundColor(headerColor)
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(8);

            table.addHeaderCell(cell);
        }

        return table;
    }
    private String money(BigDecimal amount) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.forLanguageTag("ru-RU")
                );

        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return formatter.format(amount) + " ₸";
    }
    private Cell dataCell(
            String text,
            int rowIndex,
            boolean numeric) {

        Cell cell = new Cell()
                .add(
                        new Paragraph(text == null ? "" : text)
                                .setMargin(0)
                )
                .setPadding(7)
                .setBorder(
                        new SolidBorder(
                                new DeviceRgb(226, 232, 240),
                                0.5f
                        )
                );

        if (rowIndex % 2 != 0) {
            cell.setBackgroundColor(
                    new DeviceRgb(248, 250, 252)
            );
        }

        if (numeric) {
            cell.setTextAlignment(TextAlignment.RIGHT);
        }

        return cell;
    }

    private Cell amountCell(
            BigDecimal amount,
            int rowIndex) {

        Cell cell = dataCell(
                money(amount),
                rowIndex,
                true
        );

        if (amount.signum() < 0) {
            cell.setFontColor(
                    new DeviceRgb(185, 28, 28)
            );
        }

        return cell;
    }
}