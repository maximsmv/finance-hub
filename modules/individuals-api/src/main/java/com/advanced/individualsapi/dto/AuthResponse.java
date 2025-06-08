package com.advanced.individualsapi.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        String refreshToken,
        String tokenType
) {}
