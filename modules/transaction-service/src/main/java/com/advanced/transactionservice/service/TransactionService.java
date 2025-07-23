package com.advanced.transactionservice.service;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionService {

    TransactionInitResponse initDeposit(@Valid DepositInitRequest request);

    TransactionInitResponse initWithdrawal(@Valid WithdrawalInitRequest request);

    TransactionInitResponse initTransfer(@Valid TransferInitRequest request);

    TransactionConfirmResponse confirmDeposit(@Valid DepositConfirmRequest request);

    TransactionConfirmResponse confirmWithdrawal(@Valid WithdrawalConfirmRequest request);

    TransactionConfirmResponse confirmTransfer(@Valid TransferConfirmRequest request);

    TransactionStatusResponse getTransactionStatus(String transactionId);

    List<TransactionStatusResponse> searchTransactions(
            String userUid, String walletUid,
            PaymentType type, PaymentStatus status,
            OffsetDateTime dateFrom, OffsetDateTime dateTo,
            int page, int size
    );
}
