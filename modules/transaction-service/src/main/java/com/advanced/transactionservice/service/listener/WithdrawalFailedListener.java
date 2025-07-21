package com.advanced.transactionservice.service.listener;

import com.advanced.kafkacontracts.WithdrawalFailed;
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
public class WithdrawalFailedListener {

    private final TransactionRepository repository;

    private final WalletService walletService;

    @KafkaListener(topics = "withdrawal-failed", groupId = "transaction-service")
    @Transactional
    public void handle(WithdrawalFailed payload) {
        log.info("Received withdrawal-failed event: {}", payload);

        UUID transactionUid = payload.getTransactionUid();
        Transaction transaction = repository.findById(transactionUid)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionUid));

        if (transaction.getStatus() == PaymentStatus.COMPLETED || transaction.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        walletService.credit(transaction.getWalletUid(), transaction.getAmount().add(transaction.getFee()));

        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureReason(payload.getFailureReason());
        repository.save(transaction);
    }

}
