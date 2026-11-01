
package com.globus.finance.controller.transaction;

import com.globus.finance.dto.transaction.TransactionForm;
import com.globus.finance.entity.TransactionType;
import com.globus.finance.repository.CategoryRepository;
import com.globus.finance.service.transaction.TransactionService;
import com.globus.finance.entity.Transaction;
import com.globus.finance.service.CurrentUserService;

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

    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public TransactionPageController(
            TransactionService transactionService,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService) {

        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
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
                        currentUserService.getCurrentUserId(),
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
                transactionService.getUserTransactions(currentUserService.getCurrentUserId())
        );

        model.addAttribute(
                "incomeCategories",
                categoryRepository.findByUser_IdAndType(
                        currentUserService.getCurrentUserId(),
                        TransactionType.INCOME
                )
        );

        model.addAttribute(
                "expenseCategories",
                categoryRepository.findByUser_IdAndType(
                        currentUserService.getCurrentUserId(),
                        TransactionType.EXPENSE
                )
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
                            currentUserService.getCurrentUserId(), id);

            TransactionForm form = new TransactionForm();

            form.setAmount(transaction.getAmount());
            form.setType(transaction.getType());
            form.setCategoryId(transaction.getCategory().getId());
            form.setTransactionDate(transaction.getTransactionDate());
            form.setDescription(transaction.getDescription());

            model.addAttribute("transactionForm", form);
            model.addAttribute("transactionId", id);
            model.addAttribute("categories",
                    categoryRepository.findByUser_IdOrderByTypeAscNameAsc(
                            currentUserService.getCurrentUserId()
                    ));

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
                        currentUserService.getCurrentUserId(),
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
                categoryRepository.findByUser_IdOrderByTypeAscNameAsc(
                        currentUserService.getCurrentUserId()
                ));

        return "transaction-edit";
    }

    @PostMapping("/{id}/delete")
    public String deleteTransaction(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            transactionService.deleteTransaction(
                    id, currentUserService.getCurrentUserId());

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
