package com.advanced.individualsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletRequest(
        @NotBlank(message = "name не может быть пустым")
        String name,
        @NotNull(message = "wallet_type_uid не может быть null")
        UUID walletTypeUid
) {}

