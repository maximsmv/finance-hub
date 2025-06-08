package com.advanced.individualsapi.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "REFRESH_TOKEN не может быть пустым")
        String refreshToken
) {}
