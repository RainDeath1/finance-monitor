package com.globus.finance.service.auth;

import com.globus.finance.entity.PasswordResetToken;
import com.globus.finance.entity.User;
import com.globus.finance.repository.PasswordResetTokenRepository;
import com.globus.finance.repository.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME =
            Duration.ofMinutes(30);

    private static final Duration RESEND_COOLDOWN =
            Duration.ofMinutes(5);

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher) {

        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void requestPasswordReset(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        Optional<User> optionalUser =
                userRepository.findByEmailForPasswordReset(
                        normalizedEmail
                );

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        if (!user.isEnabled() || !user.isEmailVerified()) {
            return;
        }

        Optional<PasswordResetToken> lastToken =
                tokenRepository
                        .findTopByUser_IdOrderByCreatedAtDesc(
                                user.getId()
                        );

        Instant now = Instant.now();

        if (lastToken.isPresent()) {

            PasswordResetToken previousToken = lastToken.get();

            Instant nextAllowedTime =
                    previousToken.getCreatedAt()
                            .plus(RESEND_COOLDOWN);

            if (now.isBefore(nextAllowedTime)) {
                return;
            }
        }

        tokenRepository.deleteAllByUserId(user.getId());

        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        PasswordResetToken token =
                new PasswordResetToken();

        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(
                Instant.now().plus(TOKEN_LIFETIME)
        );

        tokenRepository.save(token);


        eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(
                        user.getEmail(),
                        rawToken
                )
        );
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword) {

        if (rawToken == null
                || !rawToken.matches("[A-Za-z0-9_-]{43}")) {

            throw new IllegalArgumentException(
                    "Некорректная ссылка восстановления"
            );
        }

        if (newPassword == null
                || newPassword.isBlank()
                || newPassword.length() < 8
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Пароль должен содержать не менее 8 символов " +
                            "и не более 72 байт"
            );
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ссылка восстановления недействительна"
                        )
                );

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "Ссылка уже использована"
            );
        }

        Instant now = Instant.now();

        if (!now.isBefore(token.getExpiresAt())) {
            throw new IllegalArgumentException(
                    "Срок действия ссылки истёк"
            );
        }

        User user = token.getUser();

        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Восстановление пароля недоступно"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        user.setSessionVersion(
                user.getSessionVersion() + 1
        );
        token.setUsedAt(now);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "Алгоритм SHA-256 недоступен", e
            );
        }
    }
}