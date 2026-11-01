package com.globus.finance.service.auth;

import com.globus.finance.dto.auth.RegistrationForm;
import com.globus.finance.entity.Role;
import com.globus.finance.entity.User;
import com.globus.finance.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationForm form) {
        if (!form.password().equals(form.confirmPassword())) {
            throw new IllegalArgumentException(
                    "Пароли не совпадают"
            );
        }

        String email = form.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Пользователь с таким email уже существует"
            );
        }

        User user = new User();

        user.setName(form.name().trim());
        user.setEmail(email);
        user.setRole(Role.USER);

        user.setPasswordHash(
                passwordEncoder.encode(form.password())
        );

        userRepository.save(user);
    }
}