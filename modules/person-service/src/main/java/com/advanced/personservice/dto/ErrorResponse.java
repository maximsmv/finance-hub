package com.advanced.personservice.dto;

public record ErrorResponse(
        String error,
        int status
) {}
