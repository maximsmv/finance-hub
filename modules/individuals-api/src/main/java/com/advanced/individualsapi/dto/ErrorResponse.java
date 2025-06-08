package com.advanced.individualsapi.dto;

public record ErrorResponse(
        String error,
        int status
) {}
