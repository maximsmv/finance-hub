package com.advanced.transactionservice.repository;

import com.advanced.transactionservice.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByUserUid(UUID userUid);

    Optional<Wallet> findByUidAndUserUid(UUID uid, UUID userUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.uid = :id AND w.userUid = :userUid")
    Optional<Wallet> findForUpdate(@Param("id") UUID id, @Param("userUid") UUID userUid);
}
