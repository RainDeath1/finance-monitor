package com.globus.finance.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationForm(

        @NotBlank(message = "Введите имя")
        @Size(max = 100, message = "Имя слишком длинное")
        String name,

        @NotBlank(message = "Введите email")
        @Email(message = "Некорректный email")
        String email,

        @NotBlank(message = "Введите пароль")
        @Size(
                min = 8,
                message = "Пароль должен содержать минимум 8 символов"
        )
        String password,

        @NotBlank(message = "Подтвердите пароль")
        String confirmPassword

) {
}