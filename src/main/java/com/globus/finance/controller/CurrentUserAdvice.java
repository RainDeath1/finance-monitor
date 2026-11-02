package com.globus.finance.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.Authentication;

import java.security.Principal;

@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Principal principal) {

        return principal != null
                ? principal.getName()
                : null;
    }

    @ModelAttribute("currentUserIsAdmin")
    public boolean currentUserIsAdmin(Authentication authentication) {

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );
    }
}