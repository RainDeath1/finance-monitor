package com.globus.finance.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetrics(
        LocalDate date,
        BigDecimal income,
        BigDecimal expenses
) {
}