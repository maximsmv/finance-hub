package com.advanced.transactionservice.repository;

import com.advanced.transactionservice.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByUserUid(UUID uuid);
}
