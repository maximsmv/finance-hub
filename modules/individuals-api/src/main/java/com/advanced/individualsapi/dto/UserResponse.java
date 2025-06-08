package com.advanced.individualsapi.dto;

import java.util.List;

public record UserResponse(
        String id,
        String email,
        List<String> roles,
        String createdAt
) {}
