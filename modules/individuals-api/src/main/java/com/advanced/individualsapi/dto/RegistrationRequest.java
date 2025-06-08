package com.advanced.individualsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email,
        @NotBlank(message = "Пароль не может быть пустым")
        String password,
        @NotBlank(message = "Подтверждение пароля не может быть пустым")
        String confirmPassword
) {}

