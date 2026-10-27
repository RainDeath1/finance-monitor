package com.globus.finance.dto;

import java.math.BigDecimal;

public record MonthlyMetrics(
        int month,
        BigDecimal income,
        BigDecimal expenses
) {
}