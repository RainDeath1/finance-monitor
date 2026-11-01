package com.globus.finance.dto.report;

import java.math.BigDecimal;

public record ReportDetailRow(
        String label,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
) {
}