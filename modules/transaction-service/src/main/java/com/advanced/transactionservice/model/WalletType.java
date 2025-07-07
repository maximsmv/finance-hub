package com.advanced.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class WalletType {

    @Id
    @GeneratedValue
    private UUID uid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime modifiedAt;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 18)
    private String status;

    private OffsetDateTime archivedAt;

    private String userType;

    private String creator;

    private String modifier;

}
