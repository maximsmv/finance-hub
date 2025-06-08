package com.advanced.individualsapi.dto;

public record FieldErrorResponse(
        String field,
        String message
) {}
