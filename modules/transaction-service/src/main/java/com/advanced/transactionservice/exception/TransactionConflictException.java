package com.advanced.transactionservice.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Setter
@ResponseStatus(HttpStatus.CONFLICT)
public class TransactionConflictException extends TransactionException {
    public TransactionConflictException() {
        super("Duplicate transaction UID", HttpStatus.CONFLICT);
    }
}
