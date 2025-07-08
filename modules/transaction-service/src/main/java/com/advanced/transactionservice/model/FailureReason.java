package com.advanced.transactionservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FailureReason {
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS"),
    WALLET_NOT_FOUND("WALLET_NOT_FOUND"),
    UNKNOWN_ERROR("UNKNOWN_ERROR");

    private final String value;

    public static FailureReason fromValue(String value) {
        for(FailureReason e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }

        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public String toString() {
        return String.valueOf(this.value);
    }

}
