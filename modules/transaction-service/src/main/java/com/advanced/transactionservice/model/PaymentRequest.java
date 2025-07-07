package com.advanced.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_uid")
    private Wallet sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_wallet_uid")
    private Wallet destinationWallet;

    @Column(nullable = false, length = 20)
    private String status;

    private OffsetDateTime processedAt;

    private OffsetDateTime expiresAt;

}
