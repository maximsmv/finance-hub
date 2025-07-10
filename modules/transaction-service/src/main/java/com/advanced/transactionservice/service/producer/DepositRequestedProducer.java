package com.advanced.transactionservice.service.producer;

import com.advanced.kafka.contracts.model.DepositRequestedPayload;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepositRequestedProducer {

    private final KafkaTemplate<String, DepositRequestedPayload> kafkaTemplate;

    private final KafkaTopicsProperties topics;

    public void send(final DepositRequestedPayload payload) {
        kafkaTemplate.send(topics.getDepositRequested(), payload.getTransactionId(), payload);
    }

}
