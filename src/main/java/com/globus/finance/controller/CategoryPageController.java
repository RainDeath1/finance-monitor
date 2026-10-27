
package com.globus.finance.controller;

import com.globus.finance.dto.CategoryForm;
import com.globus.finance.service.CategoryService;
import com.globus.finance.entity.Category;

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

    public CategoryPageController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String showCategories(Model model) {

        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("categories",
                categoryService.getAllCategories());

        return "categories";
    }

    @PostMapping
    public String addCategory(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            try {
                categoryService.createCategory(
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
                categoryService.getAllCategories()
        );

        return "categories";
    }


    @PostMapping("/{id}/delete")
    public String deleteCategory(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            categoryService.deleteCategory(id);

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

        try {
            Category category =
                    categoryService.getCategoryById(id);

            CategoryForm form = new CategoryForm();

            form.setName(category.getName());
            form.setType(category.getType());

            model.addAttribute("categoryForm", form);
            model.addAttribute("categoryId", id);

            return "category-edit";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());

            return "redirect:/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("categoryForm")
            CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            try {
                categoryService.updateCategory(
                        id,
                        form.getName(),
                        form.getType()
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Категория успешно обновлена!");

                return "redirect:/categories";

            } catch (IllegalArgumentException ex) {
                bindingResult.reject(
                        "category.error",
                        ex.getMessage());
            }
        }

        model.addAttribute("categoryId", id);

        return "category-edit";
    }


}
