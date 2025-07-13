package com.advanced.transactionservice.service.listener;

import com.advanced.kafka.contracts.model.WithdrawalFailedPayload;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
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
public class WithdrawalFailedListener {

    private final PaymentRequestRepository repository;

    @KafkaListener(topics = "withdrawal-failed", groupId = "transaction-service")
    @Transactional
    public void handle(WithdrawalFailedPayload payload) {
        log.info("Received withdrawal-failed event: {}", payload);

        UUID transactionId = UUID.fromString(payload.getTransactionId());
        PaymentRequest request = repository.findByTransactionUid(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        if (request.getStatus() == PaymentStatus.COMPLETED || request.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        request.setStatus(PaymentStatus.FAILED);
        request.setFailureReason(payload.getReason().name());
        request.setProcessedAt(OffsetDateTime.now());

        repository.save(request);
    }

}
