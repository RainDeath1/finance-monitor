package com.globus.finance.service.admin;

import com.globus.finance.entity.Role;
import com.globus.finance.entity.User;
import com.globus.finance.repository.PasswordResetTokenRepository;
import com.globus.finance.repository.UserRepository;
import com.globus.finance.entity.AdminAuditAction;
import com.globus.finance.dto.auth.RegistrationForm;
import com.globus.finance.service.auth.RegistrationService;
import com.globus.finance.repository.EmailVerificationTokenRepository;

import com.globus.finance.service.CurrentUserService;
import jakarta.persistence.EntityManager;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AdminAuditService adminAuditService;
    private final RegistrationService registrationService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AdminUserService(
            UserRepository userRepository,
            CurrentUserService currentUserService,
            EntityManager entityManager,
            AdminAuditService adminAuditService,
            RegistrationService registrationService,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {

        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
        this.adminAuditService = adminAuditService;
        this.registrationService = registrationService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public void changeUserRole(Long targetUserId, Role newRole) {

        if (targetUserId == null || newRole == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь или роль"
            );
        }

        Long currentUserId =
                currentUserService.getCurrentUserId();

        List<User> admins =
                userRepository.lockUsersByRole(Role.ADMIN);

        User currentUser = admins.stream()
                .filter(admin ->
                        admin.getId().equals(currentUserId)
                                && admin.isEnabled()
                )
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Недостаточно прав для изменения ролей"
                        )
                );

        User targetUser = userRepository
                .findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"
                        )
                );

        if (targetUser.getRole() == newRole) {
            return;
        }

        if (targetUserId.equals(currentUserId)
                && newRole != Role.ADMIN) {

            throw new IllegalArgumentException(
                    "Нельзя изменить собственную роль администратора"
            );
        }

        long activeAdmins = admins.stream()
                .filter(User::isEnabled)
                .count();

        if (targetUser.getRole() == Role.ADMIN
                && targetUser.isEnabled()
                && newRole == Role.USER
                && activeAdmins <= 1) {

            throw new IllegalArgumentException(
                    "Нельзя удалить права последнего активного администратора"
            );
        }

        targetUser.setRole(newRole);

        userRepository.save(targetUser);

        adminAuditService.log(
                currentUser.getId(),
                currentUser.getEmail(),
                targetUser.getId(),
                targetUser.getEmail(),
                newRole == Role.ADMIN
                        ? AdminAuditAction.ROLE_ASSIGNED
                        : AdminAuditAction.ROLE_REMOVED,
                "Роль пользователя изменена на " + newRole.name()
        );
    }

    @Transactional
    public void changeUserStatus(Long userId, boolean enabled) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь"
            );
        }

        List<User> admins =
                userRepository.lockUsersByRole(Role.ADMIN);

        Long currentUserId =
                currentUserService.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Текущий пользователь не найден"
                        )
                );

        if (currentUser.getRole() != Role.ADMIN
                || !currentUser.isEnabled()) {

            throw new AccessDeniedException(
                    "Недостаточно прав"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"
                        )
                );

        if (user.isEnabled() == enabled) {
            return;
        }

        if (currentUserId.equals(userId) && !enabled) {
            throw new IllegalArgumentException(
                    "Нельзя заблокировать собственную учётную запись"
            );
        }

        long activeAdmins = admins.stream()
                .filter(User::isEnabled)
                .count();

        if (user.getRole() == Role.ADMIN
                && user.isEnabled()
                && !enabled
                && activeAdmins <= 1) {

            throw new IllegalArgumentException(
                    "Нельзя заблокировать последнего активного администратора"
            );
        }

        user.setEnabled(enabled);

        userRepository.save(user);

        adminAuditService.log(
                currentUser.getId(),
                currentUser.getEmail(),
                user.getId(),
                user.getEmail(),
                enabled
                        ? AdminAuditAction.USER_UNBLOCKED
                        : AdminAuditAction.USER_BLOCKED,
                enabled
                        ? "Пользователь разблокирован"
                        : "Пользователь заблокирован"
        );
    }

    @Transactional
    public void deleteUser(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Не указан пользователь"
            );
        }

        Long currentUserId =
                currentUserService.getCurrentUserId();

        if (currentUserId.equals(userId)) {
            throw new IllegalArgumentException(
                    "Нельзя удалить собственную учётную запись"
            );
        }

        List<User> admins =
                userRepository.lockUsersByRole(Role.ADMIN);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Текущий пользователь не найден"
                        )
                );

        if (currentUser.getRole() != Role.ADMIN
                || !currentUser.isEnabled()) {

            throw new AccessDeniedException(
                    "Недостаточно прав"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"
                        )
                );

        long activeAdmins = admins.stream()
                .filter(User::isEnabled)
                .count();

        if (user.getRole() == Role.ADMIN
                && user.isEnabled()
                && activeAdmins <= 1) {

            throw new IllegalArgumentException(
                    "Нельзя удалить последнего активного администратора"
            );
        }

        entityManager.flush();

        emailVerificationTokenRepository.deleteAllByUserId(userId);

        entityManager.createQuery(
                        "DELETE FROM Transaction t WHERE t.user.id = :userId"
                )
                .setParameter("userId", userId)
                .executeUpdate();

        entityManager.createQuery(
                        "DELETE FROM Report r WHERE r.user.id = :userId"
                )
                .setParameter("userId", userId)
                .executeUpdate();

        entityManager.createQuery(
                        "DELETE FROM Category c WHERE c.user.id = :userId"
                )
                .setParameter("userId", userId)
                .executeUpdate();

        passwordResetTokenRepository.deleteAllByUserId(userId);

        userRepository.delete(user);
        userRepository.flush();

        adminAuditService.log(
                currentUser.getId(),
                currentUser.getEmail(),
                user.getId(),
                user.getEmail(),
                AdminAuditAction.USER_DELETED,
                "Пользователь удалён"
        );
    }

    @Transactional
    public void createUser(RegistrationForm form, Role role) {

        if (form == null || role == null) {
            throw new IllegalArgumentException(
                    "Не указаны данные пользователя или роль"
            );
        }

        Long currentUserId =
                currentUserService.getCurrentUserId();

        User currentUser = userRepository
                .findById(currentUserId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Текущий пользователь не найден"
                        )
                );

        if (currentUser.getRole() != Role.ADMIN
                || !currentUser.isEnabled()) {

            throw new AccessDeniedException(
                    "Недостаточно прав для создания пользователей"
            );
        }

        User newUser = registrationService.register(form);

        if (role == Role.ADMIN) {

            newUser.setRole(Role.ADMIN);

            userRepository.save(newUser);
        }

        adminAuditService.log(
                currentUser.getId(),
                currentUser.getEmail(),
                newUser.getId(),
                newUser.getEmail(),
                AdminAuditAction.USER_CREATED,
                "Создан пользователь с ролью " + role.name()
        );
    }

}