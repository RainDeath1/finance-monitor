package com.globus.finance.service.auth;

import com.globus.finance.entity.User;
import com.globus.finance.repository.UserRepository;
import com.globus.finance.repository.EmailVerificationTokenRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationResendService {

    private static final Duration RESEND_INTERVAL =
            Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationService verificationService;
    private final ApplicationEventPublisher eventPublisher;

    public EmailVerificationResendService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailVerificationService verificationService,
            ApplicationEventPublisher eventPublisher) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.verificationService = verificationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void resendVerificationEmail(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null
                || !user.isEnabled()
                || user.isEmailVerified()) {
            return;
        }

        Instant now = Instant.now();

        boolean tooEarly = tokenRepository
                .findTopByUser_IdOrderByCreatedAtDesc(user.getId())
                .map(token ->
                        now.isBefore(
                                token.getCreatedAt()
                                        .plus(RESEND_INTERVAL)
                        )
                )
                .orElse(false);

        if (tooEarly) {
            return;
        }

        String rawToken =
                verificationService.createToken(user);

        eventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(
                        user.getEmail(),
                        rawToken
                )
        );
    }
}