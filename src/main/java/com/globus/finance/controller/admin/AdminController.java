package com.globus.finance.controller.admin;

import com.globus.finance.entity.Role;
import com.globus.finance.repository.UserRepository;
import com.globus.finance.service.admin.AdminUserService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final AdminUserService adminUserService;

    public AdminController(
            UserRepository userRepository,
            AdminUserService adminUserService) {

        this.userRepository = userRepository;
        this.adminUserService = adminUserService;
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {

        List<AdminUserRow> users = userRepository
                .findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(user -> new AdminUserRow(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.isEnabled(),
                        user.getRegisteredAt()
                ))
                .toList();

        long adminCount = users.stream()
                .filter(user -> user.role() == Role.ADMIN)
                .count();

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("adminCount", adminCount);

        return "admin";
    }

    @GetMapping("/admin/test")
    @ResponseBody
    public String testAdminAccess() {
        return "Доступ администратора разрешён!";
    }

    public record AdminUserRow(
            Long id,
            String name,
            String email,
            Role role,
            boolean enabled,
            LocalDateTime registeredAt
    ) { }
    @PostMapping("/admin/users/{id}/role")
    public String changeUserRole(
            @PathVariable("id") Long userId,
            @RequestParam("role") Role newRole,
            RedirectAttributes redirectAttributes) {

        try {

            adminUserService.changeUserRole(userId, newRole);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Роль пользователя успешно изменена!"
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/status")
    public String changeUserStatus(
            @PathVariable("id") Long userId,
            @RequestParam("enabled") boolean enabled,
            RedirectAttributes redirectAttributes) {

        try {

            adminUserService.changeUserStatus(userId, enabled);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    enabled
                            ? "Пользователь успешно разблокирован!"
                            : "Пользователь успешно заблокирован!"
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(
            @PathVariable("id") Long userId,
            RedirectAttributes redirectAttributes) {

        try {

            adminUserService.deleteUser(userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Пользователь успешно удалён!"
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin";
    }
}