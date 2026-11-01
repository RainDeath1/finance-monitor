package com.globus.finance.service.category;

import com.globus.finance.entity.Category;
import com.globus.finance.entity.TransactionType;
import com.globus.finance.entity.User;

import com.globus.finance.repository.CategoryRepository;
import com.globus.finance.repository.TransactionRepository;
import com.globus.finance.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {

        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь");
        }

        return categoryRepository
                .findByUser_IdOrderByTypeAscNameAsc(userId);
    }

    @Transactional
    public Category createCategory(
            Long userId,
            String name,
            TransactionType type) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Название категории обязательно");
        }

        name = name.trim();

        if (name.length() > 100) {
            throw new IllegalArgumentException(
                    "Название не должно превышать 100 символов");
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Необходимо выбрать тип категории");
        }

        if (categoryRepository
                .existsByUser_IdAndNameIgnoreCaseAndType(
                        userId, name, type)) {

            throw new IllegalArgumentException(
                    "Такая категория уже существует");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"));

        Category category = new Category();

        category.setName(name);
        category.setType(type);
        category.setUser(user);

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(
            Long userId,
            Long id) {

        if (userId == null || id == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь или категория");
        }

        return categoryRepository
                .findByIdAndUser_Id(id, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Категория не найдена"));
    }

    @Transactional
    public void deleteCategory(
            Long userId,
            Long categoryId) {

        Category category = getCategoryById(userId, categoryId);

        if (transactionRepository.existsByCategory_Id(categoryId)) {
            throw new IllegalArgumentException(
                    "Нельзя удалить категорию: " +
                            "она используется в транзакциях");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public Category updateCategory(
            Long userId,
            Long id,
            String name,
            TransactionType type) {

        Category category = getCategoryById(userId, id);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Название категории обязательно");
        }

        name = name.trim();

        if (name.length() > 100) {
            throw new IllegalArgumentException(
                    "Название не должно превышать 100 символов");
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Выберите тип категории");
        }

        if (categoryRepository
                .existsByUser_IdAndNameIgnoreCaseAndTypeAndIdNot(
                        userId, name, type, id)) {

            throw new IllegalArgumentException(
                    "Такая категория уже существует");
        }

        if (category.getType() != type &&
                transactionRepository.existsByCategory_Id(id)) {

            throw new IllegalArgumentException(
                    "Нельзя изменить тип категории: " +
                            "она используется в транзакциях");
        }

        category.setName(name);
        category.setType(type);

        return categoryRepository.save(category);
    }
}