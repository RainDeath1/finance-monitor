package com.globus.finance.service.auth;

public record EmailVerificationRequestedEvent(
        String email,
        String token
) {
}