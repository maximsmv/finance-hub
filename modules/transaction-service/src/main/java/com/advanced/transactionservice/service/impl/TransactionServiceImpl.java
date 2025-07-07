package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.service.TransactionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    @Override
    public TransactionInitResponse initDeposit(DepositInitRequest request) {
        return null;
    }

    @Override
    public TransactionInitResponse initWithdrawal(WithdrawalInitRequest request) {
        return null;
    }

    @Override
    public TransactionInitResponse initTransfer(TransferInitRequest request) {
        return null;
    }

    @Override
    public TransactionConfirmResponse confirmDeposit(DepositConfirmRequest request) {
        return null;
    }

    @Override
    public TransactionConfirmResponse confirmWithdrawal(WithdrawalConfirmRequest request) {
        return null;
    }

    @Override
    public TransactionConfirmResponse confirmTransfer(TransferConfirmRequest request) {
        return null;
    }

    @Override
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        return null;
    }

    @Override
    public List<TransactionStatusResponse> searchTransactions(String userUid, String walletUid, String type, String status, LocalDateTime dateFrom, LocalDateTime dateTo, int page, int size) {
        return List.of();
    }
}
