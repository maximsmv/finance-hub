package com.advanced.transactionservice.service;

import com.advanced.contract.model.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionInitResponse initDeposit(@Valid DepositInitRequest request);

    TransactionInitResponse initWithdrawal(@Valid WithdrawalInitRequest request);

    TransactionInitResponse initTransfer(@Valid TransferInitRequest request);

    TransactionConfirmResponse confirmDeposit(@Valid DepositConfirmRequest request);

    TransactionConfirmResponse confirmWithdrawal(@Valid WithdrawalConfirmRequest request);

    TransactionConfirmResponse confirmTransfer(@Valid TransferConfirmRequest request);

    TransactionStatusResponse getTransactionStatus(String transactionId);

    List<TransactionStatusResponse> searchTransactions(String userUid, String walletUid, String type, String status, LocalDateTime dateFrom, LocalDateTime dateTo, int page, int size);
}
