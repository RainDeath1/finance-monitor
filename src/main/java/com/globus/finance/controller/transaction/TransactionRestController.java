package com.globus.finance.controller.transaction;

import com.globus.finance.dto.transaction.TransactionRequest;
import com.globus.finance.dto.transaction.TransactionResponse;
import com.globus.finance.entity.Transaction;
import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.transaction.TransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public TransactionRestController(
            TransactionService transactionService,
            CurrentUserService currentUserService) {

        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {

        Long currentUserId =
                currentUserService.getCurrentUserId();

        Transaction transaction =
                transactionService.createTransaction(
                        currentUserId,
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
            @RequestParam(name = "userId", required = false)
            Long requestedUserId) {

        Long currentUserId =
                currentUserService.getCurrentUserId();

        validateUserId(
                requestedUserId,
                currentUserId
        );

        return transactionService
                .getUserTransactions(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateUserId(
            Long requestedUserId,
            Long currentUserId) {

        if (requestedUserId != null
                && !requestedUserId.equals(currentUserId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Нет доступа к данным другого пользователя"
            );
        }
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