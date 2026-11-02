package com.globus.finance.controller;

import com.globus.finance.service.auth.PasswordResetService;
import com.globus.finance.service.auth.PasswordResetIpRateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Controller
public class PasswordResetController {

    private static final Duration RESEND_COOLDOWN =
            Duration.ofMinutes(5);

    private static final String LAST_EMAIL =
            "PASSWORD_RESET_LAST_EMAIL";

    private static final String LAST_REQUEST =
            "PASSWORD_RESET_LAST_REQUEST";

    private final PasswordResetService passwordResetService;

    private final PasswordResetIpRateLimiter ipRateLimiter;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            PasswordResetIpRateLimiter ipRateLimiter) {

        this.passwordResetService = passwordResetService;
        this.ipRateLimiter = ipRateLimiter;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {

        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(
            @RequestParam("email") String email,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String ipAddress = request.getRemoteAddr();

        if (!ipRateLimiter.allow(ipAddress)) {
            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );

            model.addAttribute("rateLimited", true);

            return "forgot-password";
        }

        HttpSession session = request.getSession(true);

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        Instant now = Instant.now();

        String previousEmail =
                (String) session.getAttribute(LAST_EMAIL);

        Instant previousRequest =
                (Instant) session.getAttribute(LAST_REQUEST);

        boolean repeatedRequest =
                normalizedEmail.equals(previousEmail)
                        && previousRequest != null
                        && now.isBefore(
                        previousRequest.plus(RESEND_COOLDOWN)
                );

        passwordResetService.requestPasswordReset(email);

        if (repeatedRequest) {
            return "redirect:/forgot-password?retry";
        }

        session.setAttribute(
                LAST_EMAIL,
                normalizedEmail
        );

        session.setAttribute(
                LAST_REQUEST,
                now
        );

        return "redirect:/forgot-password?sent";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam(
                    value = "token",
                    required = false
            ) String token,
            Model model) {

        if (token == null
                || !token.matches("[A-Za-z0-9_-]{43}")) {

            model.addAttribute(
                    "error",
                    "Некорректная ссылка восстановления"
            );

            return "reset-password";
        }

        model.addAttribute("token", token);

        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (!password.equals(confirmPassword)) {

            model.addAttribute("token", token);

            model.addAttribute(
                    "error",
                    "Пароли не совпадают"
            );

            return "reset-password";
        }

        try {

            passwordResetService.resetPassword(
                    token,
                    password
            );

            return "redirect:/login?passwordReset";

        } catch (IllegalArgumentException e) {

            model.addAttribute("token", token);

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "reset-password";
        }
    }
}