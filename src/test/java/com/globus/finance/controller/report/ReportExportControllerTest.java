package com.globus.finance.controller.report;

import com.globus.finance.config.SecurityConfig;

import com.globus.finance.dto.analytics.FinancialMetrics;
import com.globus.finance.dto.report.ReportPeriod;

import com.globus.finance.repository.UserRepository;

import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.report.ReportService;

import com.globus.finance.service.report.export.ExcelReportExportService;
import com.globus.finance.service.report.export.PdfReportExportService;
import com.globus.finance.service.report.export.XmlReportExportService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.context.annotation.Import;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportExportController.class)
@Import(SecurityConfig.class)
class ReportExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private XmlReportExportService xmlExportService;

    @MockitoBean
    private ExcelReportExportService excelExportService;

    @MockitoBean
    private PdfReportExportService pdfExportService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    private static final Long USER_ID = 17L;

    private MockHttpServletRequestBuilder exportRequest(
            String format) {

        return get("/api/reports/export")
                .param("period", "month")
                .param("year", "2026")
                .param("month", "11")
                .param("quarter", "4")
                .param("format", format);
    }

    private ReportPeriod prepareReport() {

        ReportPeriod period = mock(ReportPeriod.class);

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(reportService.buildPeriod(
                eq("month"),
                eq(2026),
                eq(11),
                eq(4),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(period);

        when(reportService.getSummary(USER_ID, period))
                .thenReturn(
                        new FinancialMetrics(
                                new BigDecimal("450000"),
                                new BigDecimal("125000"),
                                new BigDecimal("325000")
                        )
                );

        return period;
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(exportRequest("pdf"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                reportService,
                currentUserService
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldExportPdf() throws Exception {

        ReportPeriod period = prepareReport();

        byte[] pdfContent =
                "%PDF-1.4 TEST"
                        .getBytes(StandardCharsets.UTF_8);

        when(pdfExportService.export(
                eq(period),
                any(FinancialMetrics.class),
                eq("transactions"),
                anyList(),
                anyList(),
                anyList()
        )).thenReturn(pdfContent);

        mockMvc.perform(exportRequest("pdf"))

                .andExpect(status().isOk())

                .andExpect(
                        content().contentType(
                                MediaType.APPLICATION_PDF
                        )
                )

                .andExpect(
                        header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"finance-report.pdf\""
                        )
                )

                .andExpect(content().bytes(pdfContent));

        verify(reportService)
                .getSummary(USER_ID, period);

        verify(pdfExportService).export(
                eq(period),
                any(FinancialMetrics.class),
                eq("transactions"),
                anyList(),
                anyList(),
                anyList()
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldExportExcel() throws Exception {

        ReportPeriod period = prepareReport();

        byte[] excelContent =
                "TEST EXCEL".getBytes(StandardCharsets.UTF_8);

        when(excelExportService.export(
                eq(period),
                any(FinancialMetrics.class),
                eq("transactions"),
                anyList(),
                anyList(),
                anyList()
        )).thenReturn(excelContent);

        mockMvc.perform(exportRequest("xlsx"))

                .andExpect(status().isOk())

                .andExpect(
                        content().contentType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )

                .andExpect(
                        header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"finance-report.xlsx\""
                        )
                )

                .andExpect(content().bytes(excelContent));

        verify(reportService)
                .getSummary(USER_ID, period);

        verify(excelExportService).export(
                eq(period),
                any(FinancialMetrics.class),
                eq("transactions"),
                anyList(),
                anyList(),
                anyList()
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldRejectUnsupportedFormat() throws Exception {

        prepareReport();

        mockMvc.perform(exportRequest("csv"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                pdfExportService,
                excelExportService,
                xmlExportService
        );
    }
}