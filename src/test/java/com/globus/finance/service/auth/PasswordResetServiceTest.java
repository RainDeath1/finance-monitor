package com.globus.finance.service.auth;

import com.globus.finance.entity.PasswordResetToken;
import com.globus.finance.entity.User;
import com.globus.finance.repository.PasswordResetTokenRepository;
import com.globus.finance.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User createUser() {

        User user = new User();

        user.setId(17L);
        user.setEmail("ti@gmail.com");
        user.setEnabled(true);
        user.setEmailVerified(true);

        return user;
    }

    @Test
    void shouldCreatePasswordResetToken() {

        User user = createUser();

        when(userRepository.findByEmailForPasswordReset(
                "ti@gmail.com"
        )).thenReturn(Optional.of(user));

        when(tokenRepository
                .findTopByUser_IdOrderByCreatedAtDesc(17L))
                .thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset(
                "TI@gmail.com"
        );

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(
                        PasswordResetToken.class
                );

        verify(tokenRepository).deleteAllByUserId(17L);
        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken =
                tokenCaptor.getValue();

        assertEquals(user, savedToken.getUser());
        assertEquals(64, savedToken.getTokenHash().length());

        assertTrue(
                savedToken.getExpiresAt().isAfter(
                        Instant.now().plus(Duration.ofMinutes(29))
                )
        );

        ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        PasswordResetRequestedEvent.class
                );

        verify(eventPublisher).publishEvent(
                eventCaptor.capture()
        );

        PasswordResetRequestedEvent event =
                eventCaptor.getValue();

        assertEquals("ti@gmail.com", event.email());
        assertEquals(43, event.rawToken().length());

        assertNotEquals(
                event.rawToken(),
                savedToken.getTokenHash()
        );
    }

    @Test
    void shouldBlockRepeatedRequestWithinFiveMinutes() {

        User user = createUser();

        PasswordResetToken previousToken =
                mock(PasswordResetToken.class);

        when(previousToken.getCreatedAt())
                .thenReturn(
                        Instant.now().minus(Duration.ofMinutes(1))
                );

        when(userRepository.findByEmailForPasswordReset(
                "ti@gmail.com"
        )).thenReturn(Optional.of(user));

        when(tokenRepository
                .findTopByUser_IdOrderByCreatedAtDesc(17L))
                .thenReturn(Optional.of(previousToken));

        passwordResetService.requestPasswordReset(
                "ti@gmail.com"
        );

        verify(tokenRepository, never())
                .deleteAllByUserId(anyLong());

        verify(tokenRepository, never())
                .save(any());

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldIgnoreUnknownEmail() {

        when(userRepository.findByEmailForPasswordReset(
                "unknown@example.com"
        )).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                passwordResetService.requestPasswordReset(
                        "unknown@example.com"
                )
        );

        verifyNoInteractions(
                tokenRepository,
                passwordEncoder,
                eventPublisher
        );
    }

    @Test
    void shouldResetPasswordSuccessfully() {

        String rawToken = "A".repeat(43);

        User user = createUser();

        PasswordResetToken token =
                new PasswordResetToken();

        token.setUser(user);

        token.setExpiresAt(
                Instant.now().plus(Duration.ofMinutes(10))
        );

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        when(passwordEncoder.encode("NewPassword123!"))
                .thenReturn("{bcrypt}test-hash");

        passwordResetService.resetPassword(
                rawToken,
                "NewPassword123!"
        );

        assertEquals(
                "{bcrypt}test-hash",
                user.getPasswordHash()
        );

        assertEquals(
                1L,
                user.getSessionVersion()
        );

        assertNotNull(token.getUsedAt());

        verify(passwordEncoder).encode(
                "NewPassword123!"
        );
    }

    @Test
    void shouldRejectUsedToken() {

        String rawToken = "B".repeat(43);

        PasswordResetToken token =
                new PasswordResetToken();

        token.setUsedAt(Instant.now());

        token.setExpiresAt(
                Instant.now().plus(Duration.ofMinutes(10))
        );

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(
                        rawToken,
                        "NewPassword123!"
                )
        );

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectExpiredToken() {

        String rawToken = "C".repeat(43);

        PasswordResetToken token =
                new PasswordResetToken();

        token.setExpiresAt(
                Instant.now().minus(Duration.ofMinutes(1))
        );

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        assertThrows(
                IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(
                        rawToken,
                        "NewPassword123!"
                )
        );

        verifyNoInteractions(passwordEncoder);
    }
}