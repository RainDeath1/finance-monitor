
package com.globus.finance.dto;

import java.math.BigDecimal;

public record FinancialMetrics(
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
) {
}
