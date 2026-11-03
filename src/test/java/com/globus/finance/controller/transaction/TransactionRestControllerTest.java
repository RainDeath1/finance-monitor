package com.globus.finance.controller.transaction;


import com.globus.finance.config.SecurityConfig;
import com.globus.finance.controller.transaction.TransactionRestController;
import com.globus.finance.entity.Category;
import com.globus.finance.entity.Transaction;
import com.globus.finance.entity.TransactionType;
import com.globus.finance.repository.UserRepository;
import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.transaction.TransactionService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionRestController.class)
@Import(SecurityConfig.class)
class TransactionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long userId = 17L;

    private Transaction createMockTransaction() {

        Category category = mock(Category.class);

        when(category.getId()).thenReturn(5L);
        when(category.getName()).thenReturn("Продукты");

        Transaction transaction = mock(Transaction.class);

        when(transaction.getId()).thenReturn(10L);

        when(transaction.getAmount())
                .thenReturn(new BigDecimal("1500.50"));

        when(transaction.getType())
                .thenReturn(TransactionType.EXPENSE);

        when(transaction.getCategory())
                .thenReturn(category);

        when(transaction.getTransactionDate())
                .thenReturn(LocalDate.of(2026, 11, 3));

        when(transaction.getDescription())
                .thenReturn("Покупка продуктов");

        return transaction;
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(
                        get("/api/transactions")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                transactionService,
                currentUserService
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturnUserTransactions() throws Exception {

        Transaction transaction = createMockTransaction();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(transactionService.getUserTransactions(userId))
                .thenReturn(List.of(transaction));

        mockMvc.perform(
                        get("/api/transactions")
                )
                .andExpect(status().isOk())

                .andExpect(
                        jsonPath("$[0].id").value(10)
                )

                .andExpect(
                        jsonPath("$[0].type").value("EXPENSE")
                )

                .andExpect(
                        jsonPath("$[0].categoryName").value("Продукты")
                );

        verify(transactionService)
                .getUserTransactions(userId);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldRejectForeignUserId() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(
                        get("/api/transactions")
                                .param("userId", "18")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(transactionService);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldAllowOwnUserId() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(transactionService.getUserTransactions(userId))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/transactions")
                                .param("userId", "17")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(transactionService)
                .getUserTransactions(userId);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldCreateTransaction() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        Transaction transaction = createMockTransaction();

        when(transactionService.createTransaction(
                eq(userId),
                eq(5L),
                any(BigDecimal.class),
                eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2026, 11, 3)),
                eq("Покупка продуктов")
        )).thenReturn(transaction);

        String json = """
                {
                  "categoryId": 5,
                  "amount": 1500.50,
                  "type": "EXPENSE",
                  "transactionDate": "2026-11-03",
                  "description": "Покупка продуктов"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())

                .andExpect(jsonPath("$.id").value(10))

                .andExpect(
                        jsonPath("$.type")
                                .value("EXPENSE")
                )

                .andExpect(
                        jsonPath("$.categoryId")
                                .value(5)
                );

        verify(transactionService).createTransaction(
                eq(userId),
                eq(5L),
                any(BigDecimal.class),
                eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2026, 11, 3)),
                eq("Покупка продуктов")
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturnBadRequestForInvalidCategory()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(transactionService.createTransaction(
                eq(userId),
                eq(999L),
                any(BigDecimal.class),
                eq(TransactionType.EXPENSE),
                any(LocalDate.class),
                anyString()
        )).thenThrow(
                new IllegalArgumentException(
                        "Категория не найдена или недоступна"
                )
        );

        String json = """
                {
                  "categoryId": 999,
                  "amount": 1000,
                  "type": "EXPENSE",
                  "transactionDate": "2026-11-03",
                  "description": "Проверка"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())

                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Категория не найдена или недоступна"
                                )
                );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldRejectMalformedJson() throws Exception {

        mockMvc.perform(
                        post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }
}