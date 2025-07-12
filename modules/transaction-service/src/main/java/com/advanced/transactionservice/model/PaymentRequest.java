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
@Table(name = "payment_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class PaymentRequest {

    @Id
    @GeneratedValue
    private UUID uid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime modifiedAt;

    @Column(nullable = false)
    private UUID userUid;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal fee;

    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "payment_type")
    private PaymentType type;

    @Column(name = "wallet_uid", nullable = false)
    private UUID walletUid;

    @Column(name = "target_wallet_uid")
    private UUID targetWalletUid;

    private Currency currency;

    @Column(length = 256)
    private String failureReason;

    @Column(nullable = false)
    private UUID transactionUid;

    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private OffsetDateTime processedAt;

    private OffsetDateTime expiresAt;

}
