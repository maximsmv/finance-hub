package com.advanced.transactionservice.mapper;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.model.Transaction;

import java.util.Currency;
import java.util.UUID;

public class TransactionMapper {

    public static Transaction fromDeposit(
            DepositConfirmRequest request,
            WalletResponse wallet
    ) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setFee(request.getFee());
        transaction.setWalletUid(wallet.getWalletUid());
        transaction.setUserUid(wallet.getUserUid());
        transaction.setCurrency(Currency.getInstance(request.getCurrency()));
        transaction.setType(PaymentType.DEPOSIT);
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setComment(request.getComment());
        return transaction;
    }

    public static Transaction fromWithdrawal(
            WithdrawalConfirmRequest request,
            WalletResponse wallet
    ) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setFee(request.getFee());
        transaction.setWalletUid(wallet.getWalletUid());
        transaction.setUserUid(wallet.getUserUid());
        transaction.setCurrency(Currency.getInstance(request.getCurrency()));
        transaction.setType(PaymentType.WITHDRAWAL);
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setComment(request.getComment());
        return transaction;
    }

    public static Transaction fromTransfer(
            TransferConfirmRequest request,
            WalletResponse wallet,
            UUID targetWalletUid
    ) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setFee(request.getFee());
        transaction.setWalletUid(wallet.getWalletUid());
        transaction.setUserUid(wallet.getUserUid());
        transaction.setTargetWalletUid(targetWalletUid);
        transaction.setCurrency(Currency.getInstance(request.getCurrency()));
        transaction.setType(PaymentType.TRANSFER);
        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setComment(request.getComment());
        return transaction;
    }

    public static TransactionStatusResponse toTransactionStatusResponse(Transaction transaction) {
        TransactionStatusResponse response = new TransactionStatusResponse();
        response.setTransactionUid(transaction.getUid());
        response.setType(transaction.getType().getValue());
        response.setStatus(transaction.getStatus().getValue());
        response.setWalletUid(transaction.getWalletUid());
        response.setAmount(transaction.getAmount());
        response.setFailureReason(transaction.getFailureReason());
        response.setComment(transaction.getComment());
        return response;
    }
}
