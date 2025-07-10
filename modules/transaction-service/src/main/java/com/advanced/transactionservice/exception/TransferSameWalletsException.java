package com.advanced.transactionservice.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferSameWalletsException extends RuntimeException {
    public TransferSameWalletsException() {
        super("TransferEqualsWalletException: The same wallet is specified in the transfer parameters");
    }
}
