package com.advanced.personservice.dto;

public record FieldErrorResponse(
        String field,
        String message
) {}
