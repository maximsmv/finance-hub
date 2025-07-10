package com.advanced.transactionservice.exception;

import com.advanced.transactionservice.model.WalletStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletStatusException extends RuntimeException {
    private final WalletStatus walletStatus;
    private final String walletUid;

    public WalletStatusException(WalletStatus status, String walletUid) {
        super("WalletStatusException. The current status of the wallet with uid=" + walletUid + " is " + status.getValue());
        this.walletStatus = status;
        this.walletUid = walletUid;
    }
}
