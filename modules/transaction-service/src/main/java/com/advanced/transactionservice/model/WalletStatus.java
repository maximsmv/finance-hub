package com.advanced.transactionservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletStatus {
    ACTIVE("ACTIVE"),
    BLOCKED("BLOCKED"),
    ARCHIVED("ARCHIVED");

    private final String value;

    public static WalletStatus fromValue(String value) {
        for (WalletStatus e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
