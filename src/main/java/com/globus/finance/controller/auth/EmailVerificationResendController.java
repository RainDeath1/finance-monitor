package com.globus.finance.controller.auth;

import com.globus.finance.service.auth.EmailVerificationResendService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmailVerificationResendController {

    private final EmailVerificationResendService resendService;

    public EmailVerificationResendController(
            EmailVerificationResendService resendService) {

        this.resendService = resendService;
    }

    @GetMapping("/resend-verification")
    public String resendPage() {

        return "auth/resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(
            @RequestParam("email") String email) {

        resendService.resendVerificationEmail(email);

        return "redirect:/resend-verification?sent";
    }
}