package com.advanced.transactionservice.service.metric;

import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransactionMetricsService {
    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> transactionCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> statusCounters = new ConcurrentHashMap<>();

    public TransactionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementTransactionType(String type) {
        transactionCounters
                .computeIfAbsent(type, t ->
                        Counter.builder("transaction_total")
                                .description("Total transactions by type")
                                .tag("type", t)
                                .register(meterRegistry)
                ).increment();
    }

    public void incrementTransactionStatus(String status) {
        statusCounters
                .computeIfAbsent(status, s ->
                        Counter.builder("transaction_status_total")
                                .description("Total transactions by status")
                                .tag("status", s)
                                .register(meterRegistry)
                ).increment();
    }

    public void writeMetrics(PaymentStatus status, PaymentType type) {
        incrementTransactionStatus(status.getValue());
        incrementTransactionType(type.getValue());
    }

    public void writeMetrics(PaymentStatus status) {
        incrementTransactionStatus(status.getValue());
    }
}
