package com.globus.finance.service.transaction;

import com.globus.finance.entity.Category;
import com.globus.finance.entity.Transaction;
import com.globus.finance.entity.TransactionType;
import com.globus.finance.entity.User;

import com.globus.finance.repository.CategoryRepository;
import com.globus.finance.repository.TransactionRepository;
import com.globus.finance.repository.UserRepository;

import com.globus.finance.service.transaction.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final LocalDate date =
            LocalDate.of(2026, 11, 3);

    private User createUser() {
        User user = new User();
        user.setId(17L);
        return user;
    }

    @ParameterizedTest
    @EnumSource(
            value = TransactionType.class,
            names = {"INCOME", "EXPENSE"}
    )
    void shouldCreateTransaction(TransactionType type) {

        User user = createUser();
        Category category = mock(Category.class);

        when(userRepository.findById(17L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUser_Id(5L, 17L))
                .thenReturn(Optional.of(category));

        when(category.getType()).thenReturn(type);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Transaction result = transactionService.createTransaction(
                17L,
                5L,
                new BigDecimal("1500.00"),
                type,
                date,
                "Тестовая операция"
        );

        assertNotNull(result);
        assertSame(user, result.getUser());
        assertSame(category, result.getCategory());

        assertEquals(
                new BigDecimal("1500.00"),
                result.getAmount()
        );

        assertEquals(type, result.getType());

        assertEquals(date, result.getTransactionDate());

        verify(transactionRepository)
                .save(any(Transaction.class));
    }

    @Test
    void shouldRejectZeroAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        17L,
                        5L,
                        BigDecimal.ZERO,
                        TransactionType.EXPENSE,
                        date,
                        "Расход"
                )
        );

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldRejectNegativeAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        17L,
                        5L,
                        new BigDecimal("-100"),
                        TransactionType.EXPENSE,
                        date,
                        "Расход"
                )
        );

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldRejectForeignCategory() {

        User user = createUser();

        when(userRepository.findById(17L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUser_Id(5L, 17L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        17L,
                        5L,
                        new BigDecimal("500"),
                        TransactionType.EXPENSE,
                        date,
                        "Покупка"
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldRejectCategoryTypeMismatch() {

        User user = createUser();
        Category category = mock(Category.class);

        when(userRepository.findById(17L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUser_Id(5L, 17L))
                .thenReturn(Optional.of(category));

        when(category.getType())
                .thenReturn(TransactionType.INCOME);

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        17L,
                        5L,
                        new BigDecimal("500"),
                        TransactionType.EXPENSE,
                        date,
                        "Покупка"
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldReturnOnlyUserTransactions() {

        List<Transaction> transactions =
                List.of(new Transaction());

        when(transactionRepository
                .findByUserIdOrderByTransactionDateDesc(17L))
                .thenReturn(transactions);

        List<Transaction> result =
                transactionService.getUserTransactions(17L);

        assertEquals(1, result.size());

        verify(transactionRepository)
                .findByUserIdOrderByTransactionDateDesc(17L);
    }

    @Test
    void shouldRejectForeignTransaction() {

        when(transactionRepository.findByIdAndUserId(10L, 17L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getUserTransaction(
                        17L,
                        10L
                )
        );
    }

    @Test
    void shouldUpdateOwnTransaction() {

        Transaction transaction = new Transaction();
        Category category = mock(Category.class);

        when(transactionRepository.findByIdAndUserId(10L, 17L))
                .thenReturn(Optional.of(transaction));

        when(categoryRepository.findByIdAndUser_Id(5L, 17L))
                .thenReturn(Optional.of(category));

        when(category.getType())
                .thenReturn(TransactionType.EXPENSE);

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        Transaction result = transactionService.updateTransaction(
                10L,
                17L,
                5L,
                new BigDecimal("2500"),
                TransactionType.EXPENSE,
                date,
                "Обновлённая операция"
        );

        assertSame(transaction, result);

        assertEquals(
                new BigDecimal("2500"),
                result.getAmount()
        );

        assertEquals(
                "Обновлённая операция",
                result.getDescription()
        );

        verify(transactionRepository).save(transaction);
    }

    @Test
    void shouldNotUpdateForeignTransaction() {

        when(transactionRepository.findByIdAndUserId(10L, 17L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.updateTransaction(
                        10L,
                        17L,
                        5L,
                        new BigDecimal("2500"),
                        TransactionType.EXPENSE,
                        date,
                        "Изменение"
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldDeleteOwnTransaction() {

        Transaction transaction = new Transaction();

        when(transactionRepository.findByIdAndUserId(10L, 17L))
                .thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(10L, 17L);

        verify(transactionRepository).delete(transaction);
    }

    @Test
    void shouldNotDeleteForeignTransaction() {

        when(transactionRepository.findByIdAndUserId(10L, 17L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.deleteTransaction(10L, 17L)
        );

        verify(transactionRepository, never())
                .delete(any(Transaction.class));
    }
}