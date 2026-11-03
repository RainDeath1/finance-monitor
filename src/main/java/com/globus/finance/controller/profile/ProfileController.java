package com.globus.finance.controller.profile;

import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.profile.ProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public ProfileController(
            ProfileService profileService,
            CurrentUserService currentUserService) {

        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String profile(Model model) {

        Long userId =
                currentUserService.getCurrentUserId();

        model.addAttribute(
                "user",
                profileService.getProfile(userId)
        );

        return "profile/profile";
    }

    @PostMapping("/name")
    public String updateName(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {

        try {

            Long userId =
                    currentUserService.getCurrentUserId();

            profileService.updateName(userId, name);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Имя успешно изменено"
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @RequestParam("currentPassword")
            String currentPassword,

            @RequestParam("newPassword")
            String newPassword,

            @RequestParam("confirmPassword")
            String confirmPassword,

            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        try {

            Long userId =
                    currentUserService.getCurrentUserId();

            profileService.changePassword(
                    userId,
                    currentPassword,
                    newPassword,
                    confirmPassword
            );

            Authentication authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            new SecurityContextLogoutHandler().logout(
                    request,
                    response,
                    authentication
            );

            return "redirect:/login?passwordChanged";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/profile";
        }
    }
}