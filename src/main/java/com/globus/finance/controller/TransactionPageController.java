
package com.globus.finance.controller;

import com.globus.finance.dto.TransactionForm;
import com.globus.finance.entity.TransactionType;
import com.globus.finance.repository.CategoryRepository;
import com.globus.finance.service.TransactionService;
import com.globus.finance.entity.Transaction;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/transactions")
public class TransactionPageController {

    private static final Long DEMO_USER_ID = 1L;

    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;

    public TransactionPageController(
            TransactionService transactionService,
            CategoryRepository categoryRepository) {

        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String showTransactions(Model model) {

        TransactionForm form = new TransactionForm();
        form.setTransactionDate(LocalDate.now());

        model.addAttribute("transactionForm", form);
        loadPageData(model);

        return "transactions";
    }

    @PostMapping
    public String addTransaction(
            @Valid @ModelAttribute("transactionForm")
            TransactionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            try {
                transactionService.createTransaction(
                        DEMO_USER_ID,
                        form.getCategoryId(),
                        form.getAmount(),
                        form.getType(),
                        form.getTransactionDate(),
                        form.getDescription()
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Транзакция успешно добавлена!"
                );

                return "redirect:/transactions";

            } catch (IllegalArgumentException ex) {
                bindingResult.reject(
                        "transaction.error",
                        ex.getMessage()
                );
            }
        }

        loadPageData(model);
        return "transactions";
    }

    private void loadPageData(Model model) {

        model.addAttribute(
                "transactions",
                transactionService.getUserTransactions(DEMO_USER_ID)
        );

        model.addAttribute(
                "incomeCategories",
                categoryRepository.findByType(TransactionType.INCOME)
        );

        model.addAttribute(
                "expenseCategories",
                categoryRepository.findByType(TransactionType.EXPENSE)
        );
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Transaction transaction =
                    transactionService.getUserTransaction(
                            DEMO_USER_ID, id);

            TransactionForm form = new TransactionForm();

            form.setAmount(transaction.getAmount());
            form.setType(transaction.getType());
            form.setCategoryId(transaction.getCategory().getId());
            form.setTransactionDate(transaction.getTransactionDate());
            form.setDescription(transaction.getDescription());

            model.addAttribute("transactionForm", form);
            model.addAttribute("transactionId", id);
            model.addAttribute("categories",
                    categoryRepository.findAll());

            return "transaction-edit";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
            return "redirect:/transactions";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateTransaction(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("transactionForm")
            TransactionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            try {
                transactionService.updateTransaction(
                        id,
                        DEMO_USER_ID,
                        form.getCategoryId(),
                        form.getAmount(),
                        form.getType(),
                        form.getTransactionDate(),
                        form.getDescription()
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Транзакция успешно обновлена!");

                return "redirect:/transactions";

            } catch (IllegalArgumentException ex) {
                bindingResult.reject(
                        "transaction.error",
                        ex.getMessage());
            }
        }

        model.addAttribute("transactionId", id);
        model.addAttribute("categories",
                categoryRepository.findAll());

        return "transaction-edit";
    }

    @PostMapping("/{id}/delete")
    public String deleteTransaction(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            transactionService.deleteTransaction(
                    id, DEMO_USER_ID);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Транзакция успешно удалена!");

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
        }

        return "redirect:/transactions";
    }

}
