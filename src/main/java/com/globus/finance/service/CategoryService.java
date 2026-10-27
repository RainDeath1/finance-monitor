
package com.globus.finance.service;

import com.globus.finance.entity.Category;
import com.globus.finance.entity.TransactionType;
import com.globus.finance.repository.CategoryRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.globus.finance.repository.TransactionRepository;


import java.util.List;

@Service
public class CategoryService {


    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository) {

        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }


    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {

        return categoryRepository.findAll(
                Sort.by(
                        Sort.Order.asc("type"),
                        Sort.Order.asc("name")
                )
        );
    }

    @Transactional
    public Category createCategory(
            String name,
            TransactionType type) {

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
                .existsByNameIgnoreCaseAndType(name, type)) {

            throw new IllegalArgumentException(
                    "Такая категория уже существует");
        }

        Category category = new Category();

        category.setName(name);
        category.setType(type);

        return categoryRepository.save(category);
    }


    @Transactional
    public void deleteCategory(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Категория не найдена"));

        if (transactionRepository.existsByCategory_Id(categoryId)) {
            throw new IllegalArgumentException(
                    "Нельзя удалить категорию: она используется в транзакциях");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Не указан ID категории");
        }

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Категория не найдена"));
    }

    @Transactional
    public Category updateCategory(
            Long id,
            String name,
            TransactionType type) {

        Category category = getCategoryById(id);

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
                .existsByNameIgnoreCaseAndTypeAndIdNot(
                        name, type, id)) {

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
