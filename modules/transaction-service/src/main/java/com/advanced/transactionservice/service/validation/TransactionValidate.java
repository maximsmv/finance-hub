package com.advanced.transactionservice.service.validation;

import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.exception.WalletBalanceException;
import com.advanced.transactionservice.exception.WalletStatusException;
import com.advanced.transactionservice.model.WalletStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionValidate {

    public void validateInitDepositRequest(WalletResponse wallet) {
        checkWalletStatus(wallet);
    }

    public void validateInitWithdrawalRequest(WalletResponse wallet, BigDecimal totalAmount) {
        checkWalletStatus(wallet);
        checkWalletBalance(wallet, totalAmount);
    }

    public void validateInitTransferRequest(WalletResponse fromWallet, WalletResponse toWallet, BigDecimal totalAmount) {
        checkWalletStatus(fromWallet);
        checkWalletStatus(toWallet);
        checkWalletBalance(fromWallet, totalAmount);
    }

    private static void checkWalletBalance(WalletResponse wallet, BigDecimal totalAmount) {
        if (totalAmount.min(wallet.getBalance()).compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletBalanceException(wallet.getBalance(), totalAmount);
        }
    }


    private static void checkWalletStatus(WalletResponse wallet) {
        if (!WalletStatus.ACTIVE.getValue().equalsIgnoreCase(wallet.getStatus())) {
            throw new WalletStatusException(WalletStatus.valueOf(wallet.getStatus()), wallet.getWalletUid());
        }
    }
}
