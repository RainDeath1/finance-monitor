package com.globus.finance.service.profile;

import com.globus.finance.entity.User;
import com.globus.finance.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getProfile(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Пользователь не найден"
                        ));
    }

    @Transactional
    public void updateName(Long userId, String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Имя не может быть пустым"
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "Имя не должно превышать 100 символов"
            );
        }

        User user = getProfile(userId);

        user.setName(normalizedName);

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(
            Long userId,
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        if (currentPassword == null
                || currentPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "Укажите текущий пароль"
            );
        }

        if (newPassword == null
                || newPassword.length() < 8
                || newPassword.length() > 72) {

            throw new IllegalArgumentException(
                    "Новый пароль должен содержать от 8 до 72 символов"
            );
        }

        if (!newPassword.equals(confirmPassword)) {

            throw new IllegalArgumentException(
                    "Новые пароли не совпадают"
            );
        }

        User user = getProfile(userId);

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Неверный текущий пароль"
            );
        }

        if (passwordEncoder.matches(
                newPassword,
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Новый пароль должен отличаться от текущего"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );
        user.setSessionVersion(
                user.getSessionVersion() + 1
        );

        userRepository.save(user);
    }
}