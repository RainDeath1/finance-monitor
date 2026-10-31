package com.globus.finance.dto;

import java.math.BigDecimal;

public record ReportDetailRow(
        String label,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
) {
}