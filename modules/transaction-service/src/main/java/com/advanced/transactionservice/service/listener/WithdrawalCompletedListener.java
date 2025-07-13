package com.advanced.transactionservice.service.listener;

import com.advanced.kafka.contracts.model.WithdrawalCompletedPayload;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
import com.advanced.transactionservice.service.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCompletedListener {

    private final PaymentRequestRepository repository;

    private final WalletService walletService;

    @KafkaListener(topics = "withdrawal-completed", groupId = "transaction-service")
    @Transactional
    public void handle(WithdrawalCompletedPayload payload) {
        log.info("Received withdrawal-completed event: {}", payload);

        UUID transactionId = UUID.fromString(payload.getTransactionId());
        PaymentRequest request = repository.findByTransactionUid(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (request.getStatus() == PaymentStatus.COMPLETED || request.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        walletService.withdraw(request.getWalletUid(), request.getTotalAmount());

        request.setStatus(PaymentStatus.COMPLETED);
        request.setProcessedAt(OffsetDateTime.now());

        repository.save(request);
    }

}
