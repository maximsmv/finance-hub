package com.advanced.transactionservice.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class TransactionException extends RuntimeException {
    private final HttpStatus status;
    public TransactionException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
