package com.advanced.transactionservice.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class TransferSameWalletsException extends TransactionException {
    public TransferSameWalletsException() {
        super("The same wallet is specified in the transfer parameters", HttpStatus.BAD_REQUEST);
    }
}
