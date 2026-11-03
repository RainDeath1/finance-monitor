package com.globus.finance.service.auth;

import com.globus.finance.entity.EmailVerificationToken;
import com.globus.finance.entity.User;
import com.globus.finance.repository.EmailVerificationTokenRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void shouldCreateVerificationToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setEmailVerified(false);

        String rawToken =
                emailVerificationService.createToken(user);

        assertNotNull(rawToken);
        assertEquals(43, rawToken.length());

        ArgumentCaptor<EmailVerificationToken> captor =
                ArgumentCaptor.forClass(
                        EmailVerificationToken.class
                );

        verify(tokenRepository).deleteAllByUserId(1L);
        verify(tokenRepository).save(captor.capture());

        EmailVerificationToken savedToken =
                captor.getValue();

        assertEquals(user, savedToken.getUser());

        // В базе должен храниться хеш, а не исходный токен
        assertNotEquals(
                rawToken,
                savedToken.getTokenHash()
        );

        assertEquals(
                64,
                savedToken.getTokenHash().length()
        );

        // Срок действия — приблизительно 24 часа
        assertTrue(
                savedToken.getExpiresAt().isAfter(
                        Instant.now().plus(Duration.ofHours(23))
                )
        );
    }

    @Test
    void shouldVerifyEmailSuccessfully() {

        String rawToken = "A".repeat(43);

        User user = new User();
        user.setId(1L);
        user.setEmailVerified(false);

        EmailVerificationToken token =
                new EmailVerificationToken();

        token.setUser(user);

        token.setExpiresAt(
                Instant.now().plus(Duration.ofMinutes(10))
        );

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        emailVerificationService.verifyToken(rawToken);

        assertTrue(user.isEmailVerified());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void shouldRejectInvalidToken() {

        String invalidToken = "invalid-token";

        assertThrows(
                IllegalArgumentException.class,
                () -> emailVerificationService.verifyToken(
                        invalidToken
                )
        );

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void shouldRejectUsedToken() {

        String rawToken = "B".repeat(43);

        User user = new User();
        user.setId(1L);
        user.setEmailVerified(false);

        EmailVerificationToken token =
                new EmailVerificationToken();

        token.setUser(user);

        token.setExpiresAt(
                Instant.now().plus(Duration.ofMinutes(10))
        );

        token.setUsedAt(
                Instant.now().minus(Duration.ofMinutes(1))
        );

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                IllegalArgumentException.class,
                () -> emailVerificationService.verifyToken(rawToken)
        );

        assertFalse(user.isEmailVerified());
    }
}