package com.advanced.transactionservice.exception;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletBalanceException extends WalletException {
    private final BigDecimal balance;
    private final BigDecimal amount;

    public WalletBalanceException(BigDecimal balance, BigDecimal amount) {
        super("WalletBalanceException: balance=" + balance + ", amount=" + amount);
        this.balance = balance;
        this.amount = amount;
    }
}
