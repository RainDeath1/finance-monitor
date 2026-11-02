package com.globus.finance.service.auth;

import com.globus.finance.entity.EmailVerificationToken;
import com.globus.finance.entity.User;
import com.globus.finance.repository.EmailVerificationTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_LIFETIME =
            Duration.ofHours(24);

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository) {

        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String createToken(User user) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Пользователь не найден"
            );
        }

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Электронная почта уже подтверждена"
            );
        }
        tokenRepository.deleteAllByUserId(user.getId());

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        EmailVerificationToken token =
                new EmailVerificationToken();

        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(
                Instant.now().plus(TOKEN_LIFETIME)
        );

        tokenRepository.save(token);

        return rawToken;
    }

    @Transactional
    public void verifyToken(String rawToken) {

        if (rawToken == null ||
                !rawToken.matches("[A-Za-z0-9_-]{43}")) {

            throw new IllegalArgumentException(
                    "Некорректный токен подтверждения"
            );
        }

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ссылка подтверждения недействительна"
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

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Электронная почта уже подтверждена"
            );
        }

        user.setEmailVerified(true);
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