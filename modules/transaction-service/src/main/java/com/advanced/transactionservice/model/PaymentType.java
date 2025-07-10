package com.advanced.transactionservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {
    DEPOSIT("DEPOSIT"),
    WITHDRAWAL("WITHDRAWAL"),
    TRANSFER("TRANSFER");

    private final String value;

    public static PaymentType fromValue(String value) {
        for(PaymentType e : values()) {
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
