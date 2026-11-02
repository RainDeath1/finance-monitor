package com.globus.finance.service.auth;

public record PasswordResetRequestedEvent(
        String email,
        String rawToken
) {
}