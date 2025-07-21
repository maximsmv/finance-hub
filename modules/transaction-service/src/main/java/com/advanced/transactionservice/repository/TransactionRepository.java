package com.advanced.transactionservice.repository;

import com.advanced.transactionservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}
