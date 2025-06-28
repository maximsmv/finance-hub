package com.advanced.individualsapi.dto;

import com.advanced.contract.model.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
        @Valid
        UserDto user,
        @NotBlank(message = "Пароль не может быть пустым")
        String password,
        @NotBlank(message = "Подтверждение пароля не может быть пустым")
        String confirmPassword
) {}

