package com.advanced.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"uid"})
public class Transaction {

    @Id
    @GeneratedValue
    private UUID uid;

    @Column(name = "wallet_uid", nullable = false)
    private UUID walletUid;

    @Column(nullable = false, updatable = false)
    private UUID userUid;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "payment_type")
    private PaymentType type;

    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "target_wallet_uid")
    private UUID targetWalletUid;

    private Currency currency;

    private String comment;

    @Column(length = 256)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime modifiedAt;

}
