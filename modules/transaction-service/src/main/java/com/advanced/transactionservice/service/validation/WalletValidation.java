package com.advanced.transactionservice.service.validation;

import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.exception.TransferSameWalletsException;
import com.advanced.transactionservice.exception.WalletBalanceException;
import com.advanced.transactionservice.exception.WalletStatusException;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class WalletValidation {

    public static void checkWalletBalance(WalletResponse wallet, BigDecimal totalAmount) {
        checkWalletBalance(Objects.requireNonNull(wallet.getBalance()), totalAmount);
    }

    public static void checkWalletBalance(Wallet wallet, BigDecimal totalAmount) {
        checkWalletBalance(wallet.getBalance(), totalAmount);
    }

    public static void checkWalletStatus(WalletResponse wallet) {
        checkWalletStatus(wallet.getStatus(), wallet.getWalletUid());
    }

    public static void checkWalletStatus(Wallet wallet) {
        checkWalletStatus(wallet.getStatus().getValue(), wallet.getUid());
    }

    private static void checkWalletBalance(BigDecimal balance, BigDecimal totalAmount) {
        if (balance.compareTo(totalAmount) < 0) {
            throw new WalletBalanceException(totalAmount, totalAmount);
        }
    }

    public static void checkWalletStatus(String status, UUID uid) {
        if (!WalletStatus.ACTIVE.getValue().equalsIgnoreCase(status)) {
            throw new WalletStatusException(WalletStatus.valueOf(status), uid);
        }
    }

    public static void validateWithdrawal(Wallet wallet, BigDecimal amount) {
        WalletValidation.checkWalletStatus(wallet);
        WalletValidation.checkWalletBalance(wallet, amount);
    }

    public static void validateWithdrawal(WalletResponse wallet, BigDecimal amount) {
        WalletValidation.checkWalletStatus(wallet);
        WalletValidation.checkWalletBalance(wallet, amount);
    }

    public static void validateDeposit(Wallet wallet) {
        WalletValidation.checkWalletStatus(wallet);
    }

    public static void validateDeposit(WalletResponse wallet) {
        WalletValidation.checkWalletStatus(wallet);
    }

    public static void validateTransfer(Wallet fromWallet, Wallet toWallet, BigDecimal totalAmount) {
        checkEqualsWallet(fromWallet, toWallet);
        checkWalletStatus(fromWallet);
        checkWalletStatus(toWallet);
        checkWalletBalance(fromWallet, totalAmount);
    }

    public static void validateTransfer(WalletResponse fromWallet, WalletResponse toWallet, BigDecimal totalAmount) {
        checkEqualsWallet(fromWallet, toWallet);
        checkWalletStatus(fromWallet);
        checkWalletStatus(toWallet);
        checkWalletBalance(fromWallet, totalAmount);
    }

    private static void checkEqualsWallet(Wallet fromWallet, Wallet toWallet) {
        if (Objects.equals(fromWallet.getUid(), toWallet.getUid())) {
            throw new TransferSameWalletsException();
        }
    }

    private static void checkEqualsWallet(WalletResponse fromWallet, WalletResponse toWallet) {
        if (Objects.equals(fromWallet.getWalletUid(), toWallet.getWalletUid())) {
            throw new TransferSameWalletsException();
        }
    }

}
