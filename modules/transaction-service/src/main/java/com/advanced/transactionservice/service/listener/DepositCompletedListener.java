package com.advanced.transactionservice.service.listener;

import com.advanced.kafkacontracts.DepositCompleted;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.Transaction;
import com.advanced.transactionservice.repository.TransactionRepository;
import com.advanced.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositCompletedListener {

    private final TransactionRepository repository;

    private final WalletService walletService;

    @KafkaListener(topics = "deposit-completed", groupId = "transaction-service")
    @Transactional
    public void handle(DepositCompleted payload) {
        log.info("Received deposit-completed event: {}", payload);

        UUID transactionUid = payload.getTransactionUid();
        Transaction transaction = repository.findById(transactionUid)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionUid));

        if (transaction.getStatus() == PaymentStatus.COMPLETED || transaction.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        walletService.credit(transaction.getWalletUid(), transaction.getUserUid(), transaction.getAmount().subtract(transaction.getFee()));
        transaction.setStatus(PaymentStatus.COMPLETED);
        repository.saveAndFlush(transaction);
    }
}
