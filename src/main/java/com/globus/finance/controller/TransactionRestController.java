
package com.globus.finance.controller;

import com.globus.finance.dto.*;
import com.globus.finance.entity.Transaction;
import com.globus.finance.service.TransactionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;

    public TransactionRestController(
            TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction =
                transactionService.createTransaction(
                        request.userId(),
                        request.categoryId(),
                        request.amount(),
                        request.type(),
                        request.transactionDate(),
                        request.description()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(transaction));
    }

    @GetMapping
    public List<TransactionResponse> getAll(
            @RequestParam Long userId) {

        return transactionService
                .getUserTransactions(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleError(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getType(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getTransactionDate(),
                t.getDescription()
        );
    }
}
