
package com.globus.finance.config;

import com.globus.finance.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.DispatcherType;

import com.globus.finance.repository.UserRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Order(1)

    public SecurityFilterChain apiSecurity(
            HttpSecurity http) throws Exception {

        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurity(
            HttpSecurity http,
            UserRepository userRepository) throws Exception {

        SavedRequestAwareAuthenticationSuccessHandler defaultSuccessHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();

        defaultSuccessHandler.setDefaultTargetUrl("/dashboard");
        defaultSuccessHandler.setAlwaysUseDefaultTargetUrl(true);

        http
                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        .requestMatchers(
                                "/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/verify-email",
                                "/resend-verification",
                                "/registration-pending",
                                "/forgot-password",
                                "/reset-password"
                        ).permitAll()

                        .requestMatchers(
                                "/admin",
                                "/admin/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")

                        .successHandler((request, response, authentication) -> {

                            User user = userRepository
                                    .findByEmail(authentication.getName())
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Авторизованный пользователь не найден"
                                            )
                                    );

                            request.getSession(true).setAttribute(
                                    ActiveAccountFilter.SESSION_VERSION_ATTRIBUTE,
                                    user.getSessionVersion()
                            );

                            // Сохраняем стандартное перенаправление
                            defaultSuccessHandler.onAuthenticationSuccess(
                                    request,
                                    response,
                                    authentication
                            );
                        })

                        .permitAll()
                )

                .httpBasic(Customizer.withDefaults());

        http.addFilterBefore(
                new ActiveAccountFilter(userRepository),
                AuthorizationFilter.class
        );

        return http.build();
    }
}
