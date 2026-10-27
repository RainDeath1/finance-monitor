
package com.globus.finance.dto;

import com.globus.finance.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(

        @NotNull
        Long userId,

        @NotNull
        Long categoryId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        TransactionType type,

        @NotNull
        LocalDate transactionDate,

        @Size(max = 500)
        String description

)
{
}
