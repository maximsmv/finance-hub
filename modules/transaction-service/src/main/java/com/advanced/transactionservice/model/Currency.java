package com.advanced.transactionservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Currency {
    RUB("RUB"),
    USD("USD"),
    EUR("EUR");

    private final String value;

    public static Currency fromValue(String value) {
        for (Currency e : values()) {
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
