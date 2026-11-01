
package com.globus.finance.dto.transaction;

import com.globus.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(

        Long id,
        BigDecimal amount,
        TransactionType type,
        Long categoryId,
        String categoryName,
        LocalDate transactionDate,
        String description

) {
}
