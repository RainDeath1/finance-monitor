
package com.globus.finance.service;

import com.globus.finance.entity.*;
import com.globus.finance.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository) {

        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Transaction createTransaction(
            Long userId,
            Long categoryId,
            BigDecimal amount,
            TransactionType type,
            LocalDate date,
            String description) {

        if (userId == null || categoryId == null) {
            throw new IllegalArgumentException(
                    "Пользователь и категория обязательны");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Сумма должна быть больше нуля");
        }

        if (type == null || date == null) {
            throw new IllegalArgumentException(
                    "Тип и дата операции обязательны");
        }

        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException(
                    "Описание не должно превышать 500 символов");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Категория не найдена"));

        if (category.getType() != type) {
            throw new IllegalArgumentException(
                    "Тип категории не соответствует типу операции");
        }

        Transaction transaction = new Transaction();

        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactions(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Идентификатор пользователя обязателен");
        }

        return transactionRepository
                .findByUserIdOrderByTransactionDateDesc(userId);
    }


    @Transactional(readOnly = true)
    public Transaction getUserTransaction(
            Long userId,
            Long transactionId) {

        if (userId == null || transactionId == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь или транзакция");
        }

        return transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Транзакция не найдена"));
    }

    @Transactional
    public Transaction updateTransaction(
            Long transactionId,
            Long userId,
            Long categoryId,
            BigDecimal amount,
            TransactionType type,
            LocalDate date,
            String description) {

        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "Выберите категорию");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Сумма должна быть больше нуля");
        }

        if (type == null || date == null) {
            throw new IllegalArgumentException(
                    "Тип и дата обязательны");
        }

        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException(
                    "Описание слишком длинное");
        }

        Transaction transaction =
                getUserTransaction(userId, transactionId);

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Категория не найдена"));

        if (category.getType() != type) {
            throw new IllegalArgumentException(
                    "Тип категории не соответствует типу операции");
        }

        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(
            Long transactionId,
            Long userId) {

        Transaction transaction =
                getUserTransaction(userId, transactionId);

        transactionRepository.delete(transaction);
    }

}
