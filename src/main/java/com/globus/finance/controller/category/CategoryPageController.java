package com.globus.finance.controller.category;

import com.globus.finance.dto.category.CategoryForm;
import com.globus.finance.entity.Category;
import com.globus.finance.service.category.CategoryService;
import com.globus.finance.service.CurrentUserService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryPageController {

    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public CategoryPageController(
            CategoryService categoryService,
            CurrentUserService currentUserService) {

        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String showCategories(Model model) {

        Long userId = currentUserService.getCurrentUserId();

        model.addAttribute(
                "categoryForm",
                new CategoryForm()
        );

        model.addAttribute(
                "categories",
                categoryService.getAllCategories(userId)
        );

        return "category/categories";
    }

    @PostMapping
    public String addCategory(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = currentUserService.getCurrentUserId();

        if (!bindingResult.hasErrors()) {
            try {

                categoryService.createCategory(
                        userId,
                        form.getName(),
                        form.getType()
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Категория успешно добавлена!"
                );

                return "redirect:/categories";

            } catch (IllegalArgumentException ex) {

                bindingResult.reject(
                        "category.error",
                        ex.getMessage()
                );
            }
        }

        model.addAttribute(
                "categories",
                categoryService.getAllCategories(userId)
        );

        return "category/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        Long userId = currentUserService.getCurrentUserId();

        try {

            categoryService.deleteCategory(userId, id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Категория успешно удалена!"
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = currentUserService.getCurrentUserId();

        try {

            Category category =
                    categoryService.getCategoryById(userId, id);

            CategoryForm form = new CategoryForm();

            form.setName(category.getName());
            form.setType(category.getType());

            model.addAttribute("categoryForm", form);
            model.addAttribute("categoryId", id);

            return "category/category-edit";

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = currentUserService.getCurrentUserId();

        if (!bindingResult.hasErrors()) {
            try {

                categoryService.updateCategory(
                        userId,
                        id,
                        form.getName(),
                        form.getType()
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Категория успешно обновлена!"
                );

                return "redirect:/categories";

            } catch (IllegalArgumentException ex) {

                bindingResult.reject(
                        "category.error",
                        ex.getMessage()
                );
            }
        }

        model.addAttribute("categoryId", id);

        return "category/category-edit";
    }
}