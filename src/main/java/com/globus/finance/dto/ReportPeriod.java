package com.globus.finance.dto;

import java.time.LocalDate;

public record ReportPeriod(
        LocalDate startDate,
        LocalDate endDate,
        String title
) {
}