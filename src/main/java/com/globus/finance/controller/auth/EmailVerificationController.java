package com.globus.finance.controller.auth;

import com.globus.finance.service.auth.EmailVerificationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService) {

        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/verify-email")
    public String verificationPage(
            @RequestParam(name = "token", required = false)
            String token,
            Model model) {

        if (token == null || token.isBlank()) {

            model.addAttribute(
                    "error",
                    "Отсутствует токен подтверждения"
            );

            return "auth/verify-email";
        }

        model.addAttribute("token", token);

        return "auth/verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(
            @RequestParam("token") String token,
            Model model) {

        try {

            emailVerificationService.verifyToken(token);

            model.addAttribute(
                    "success",
                    "Электронная почта успешно подтверждена!"
            );

        } catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "auth/verify-email";
    }
}