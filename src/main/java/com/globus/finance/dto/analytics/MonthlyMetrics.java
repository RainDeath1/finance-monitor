package com.globus.finance.dto.analytics;

import java.math.BigDecimal;

public record MonthlyMetrics(
        int month,
        BigDecimal income,
        BigDecimal expenses
) {
}