package com.globus.finance.service.admin;

import com.globus.finance.entity.Role;
import com.globus.finance.entity.User;
import com.globus.finance.repository.UserRepository;

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

    public AdminUserService(
            UserRepository userRepository,
            CurrentUserService currentUserService,
            EntityManager entityManager) {

        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
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

        boolean isAdmin = admins.stream()
                .anyMatch(admin ->
                        admin.getId().equals(currentUserId)
                );

        if (!isAdmin) {
            throw new AccessDeniedException(
                    "Недостаточно прав для изменения ролей"
            );
        }

        User targetUser = userRepository
                .findById(targetUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"
                        )
                );

        if (targetUserId.equals(currentUserId)
                && newRole != Role.ADMIN) {

            throw new IllegalArgumentException(
                    "Нельзя изменить собственную роль администратора"
            );
        }

        if (targetUser.getRole() == Role.ADMIN
                && newRole == Role.USER
                && admins.size() <= 1) {

            throw new IllegalArgumentException(
                    "Нельзя удалить права последнего администратора"
            );
        }

        targetUser.setRole(newRole);

        userRepository.save(targetUser);
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

        userRepository.delete(user);
        userRepository.flush();
    }
}