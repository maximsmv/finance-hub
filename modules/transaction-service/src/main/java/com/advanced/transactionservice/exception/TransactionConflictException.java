package com.advanced.transactionservice.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class TransactionConflictException extends TransactionException {
    public TransactionConflictException() {
        super("Duplicate transaction UID", HttpStatus.CONFLICT);
    }
}
