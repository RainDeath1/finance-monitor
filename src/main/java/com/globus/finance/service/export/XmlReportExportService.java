package com.globus.finance.service.export;

import com.globus.finance.dto.CategoryReportRow;
import com.globus.finance.dto.FinancialMetrics;
import com.globus.finance.dto.ReportDetailRow;
import com.globus.finance.dto.ReportPeriod;
import com.globus.finance.entity.Transaction;

import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class XmlReportExportService {

    public byte[] export(
            ReportPeriod period,
            FinancialMetrics summary,
            String detail,
            List<Transaction> transactions,
            List<ReportDetailRow> groupedDetails,
            List<CategoryReportRow> categories) {

        StringWriter output = new StringWriter();

        try {
            XMLStreamWriter xml =
                    XMLOutputFactory.newFactory()
                            .createXMLStreamWriter(output);

            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("financialReport");

            xml.writeStartElement("period");

            writeElement(xml, "title", period.title());
            writeElement(xml, "startDate",
                    period.startDate().toString());

            writeElement(xml, "endDate",
                    period.endDate().minusDays(1).toString());

            xml.writeEndElement();

            writeElement(xml, "currency", "KZT");

            xml.writeStartElement("summary");

            writeElement(xml, "income",
                    summary.income().toPlainString());

            writeElement(xml, "expenses",
                    summary.expenses().toPlainString());

            writeElement(xml, "balance",
                    summary.balance().toPlainString());

            xml.writeEndElement();

            xml.writeStartElement("details");
            xml.writeAttribute("type", detail);

            if ("transactions".equals(detail)) {

                for (Transaction transaction : transactions) {

                    xml.writeStartElement("transaction");

                    writeElement(xml, "date",
                            transaction.getTransactionDate().toString());

                    writeElement(xml, "category",
                            transaction.getCategory().getName());

                    writeElement(xml, "type",
                            transaction.getType().name());

                    writeElement(xml, "amount",
                            transaction.getAmount().toPlainString());

                    writeElement(xml, "description",
                            transaction.getDescription() == null
                                    ? ""
                                    : transaction.getDescription());

                    xml.writeEndElement();
                }

            } else if ("days".equals(detail)
                    || "months".equals(detail)) {

                for (ReportDetailRow row : groupedDetails) {

                    xml.writeStartElement("row");

                    writeElement(xml, "period", row.label());

                    writeElement(xml, "income",
                            row.income().toPlainString());

                    writeElement(xml, "expenses",
                            row.expenses().toPlainString());

                    writeElement(xml, "balance",
                            row.balance().toPlainString());

                    xml.writeEndElement();
                }

            } else {
                throw new IllegalArgumentException(
                        "Неизвестный тип детализации");
            }

            xml.writeEndElement();

            xml.writeStartElement("categories");

            for (CategoryReportRow category : categories) {

                xml.writeStartElement("category");

                writeElement(xml, "name", category.category());

                writeElement(xml, "type",
                        category.type().name());

                writeElement(xml, "amount",
                        category.amount().toPlainString());

                xml.writeEndElement();
            }

            xml.writeEndElement();

            xml.writeEndElement();
            xml.writeEndDocument();

            xml.flush();
            xml.close();

            return output.toString()
                    .getBytes(StandardCharsets.UTF_8);

        } catch (XMLStreamException e) {
            throw new IllegalStateException(
                    "Ошибка формирования XML-отчёта", e);
        }
    }

    private void writeElement(
            XMLStreamWriter xml,
            String name,
            String value) throws XMLStreamException {

        xml.writeStartElement(name);
        xml.writeCharacters(value);
        xml.writeEndElement();
    }
}