package com.advanced.transactionservice.repository;

import com.advanced.transactionservice.model.TransferOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferOperationRepository extends JpaRepository<TransferOperation, UUID> {
    TransferOperation findByTransactionUid(UUID transactionUid);
}
