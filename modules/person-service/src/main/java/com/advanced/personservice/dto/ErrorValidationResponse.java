package com.advanced.personservice.dto;

import java.util.List;

public record ErrorValidationResponse(
        String message,
        int status,
        List<FieldErrorResponse> errors
) {}