
package com.globus.finance.dto.analytics;

import java.math.BigDecimal;

public record FinancialMetrics(
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
) {
}
