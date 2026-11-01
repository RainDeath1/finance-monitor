package com.globus.finance.dto.report;

import com.globus.finance.entity.TransactionType;

import java.math.BigDecimal;

public record CategoryReportRow(
        String category,
        TransactionType type,
        BigDecimal amount
) {
}