package com.advanced.transactionservice.service.listener;

import com.advanced.kafkacontracts.WithdrawalCompleted;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.Transaction;
import com.advanced.transactionservice.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCompletedListener {

    private final TransactionRepository repository;

    @KafkaListener(topics = "withdrawal-completed", groupId = "transaction-service")
    @Transactional
    public void handle(WithdrawalCompleted payload) {
        log.info("Received withdrawal-completed event: {}", payload);

        UUID transactionUid = payload.getTransactionUid();
        Transaction transaction = repository.findById(transactionUid)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionUid));

        if (transaction.getStatus() == PaymentStatus.COMPLETED || transaction.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        transaction.setStatus(PaymentStatus.COMPLETED);
        repository.saveAndFlush(transaction);
    }

}
