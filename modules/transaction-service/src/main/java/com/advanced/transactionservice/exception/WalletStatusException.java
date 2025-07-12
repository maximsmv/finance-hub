package com.advanced.transactionservice.exception;

import com.advanced.transactionservice.model.WalletStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class WalletStatusException extends WalletException {
    private final WalletStatus walletStatus;
    private final UUID walletUid;

    public WalletStatusException(WalletStatus status, UUID walletUid) {
        super("WalletStatusException. The current status of the wallet with uid=" + walletUid + " is " + status.getValue());
        this.walletStatus = status;
        this.walletUid = walletUid;
    }
}
