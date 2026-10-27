package com.globus.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetrics(
        LocalDate date,
        BigDecimal income,
        BigDecimal expenses
) {
}