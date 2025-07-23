package com.advanced.transactionservice.repository.specification;

import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.model.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecifications {

    public static Specification<Transaction> withFilters(
            String userUid,
            String walletUid,
            PaymentType type,
            PaymentStatus status,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userUid != null) {
                predicates.add(cb.equal(root.get("userUid"), UUID.fromString(userUid)));
            }
            if (walletUid != null) {
                predicates.add(cb.equal(root.get("walletUid"), UUID.fromString(walletUid)));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
