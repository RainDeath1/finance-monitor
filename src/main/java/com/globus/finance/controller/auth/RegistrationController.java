package com.globus.finance.controller.auth;

import com.globus.finance.dto.auth.RegistrationForm;
import com.globus.finance.service.auth.RegistrationService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(
            RegistrationService registrationService) {

        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String registrationPage(Model model) {

        model.addAttribute(
                "form",
                new RegistrationForm("", "", "", "")
        );

        return "auth/register";
    }

    @GetMapping("/registration-pending")
    public String registrationPendingPage() {
        return "auth/registration-pending";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {

            registrationService.register(form);

            return "redirect:/registration-pending";

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            return "auth/register";
        }
    }
}