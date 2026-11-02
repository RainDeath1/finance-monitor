package com.globus.finance.config;

import com.globus.finance.entity.User;
import com.globus.finance.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ActiveAccountFilter extends OncePerRequestFilter {

    public static final String SESSION_VERSION_ATTRIBUTE =
            "FINANCE_MONITOR_SESSION_VERSION";

    private final UserRepository userRepository;

    public ActiveAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        boolean validAccount =
                user != null
                        && user.isEnabled()
                        && user.isEmailVerified();

        boolean validRole =
                validAccount
                        && authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals(
                                        "ROLE_" + user.getRole().name()
                                )
                        );

        HttpSession session = request.getSession(false);

        boolean validSessionVersion = false;

        if (validAccount && session != null) {

            Object storedVersion =
                    session.getAttribute(
                            SESSION_VERSION_ATTRIBUTE
                    );

            if (storedVersion instanceof Long version) {

                validSessionVersion =
                        version == user.getSessionVersion();
            }
        }

        if (!validAccount
                || !validRole
                || !validSessionVersion) {

            new SecurityContextLogoutHandler()
                    .logout(request, response, authentication);

            response.sendRedirect(
                    request.getContextPath()
                            + "/login?sessionExpired"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}