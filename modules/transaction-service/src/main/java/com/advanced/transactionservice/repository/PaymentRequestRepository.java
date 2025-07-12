package com.advanced.transactionservice.repository;

import com.advanced.transactionservice.model.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {
    Optional<PaymentRequest> findByTransactionUid(UUID transactionUid);
}
