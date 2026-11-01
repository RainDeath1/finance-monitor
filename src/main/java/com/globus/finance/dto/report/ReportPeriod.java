package com.globus.finance.dto.report;

import java.time.LocalDate;

public record ReportPeriod(
        LocalDate startDate,
        LocalDate endDate,
        String title
) {
}