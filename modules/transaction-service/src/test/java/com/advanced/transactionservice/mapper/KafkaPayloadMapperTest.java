package com.advanced.transactionservice.mapper;

import com.advanced.transactionservice.model.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KafkaPayloadMapperTest {

    private final KafkaPayloadMapper mapper = Mappers.getMapper(KafkaPayloadMapper.class);

    @Test
    void toDepositRequestedPayload_shouldMapCorrectly() {
        var payment = new PaymentRequest();
        payment.setUid(UUID.randomUUID());
        payment.setWalletUid(UUID.randomUUID());
        payment.setUserUid(UUID.randomUUID());
        payment.setCurrency(Currency.getInstance("RUB"));
        payment.setTotalAmount(new BigDecimal("99.99"));
        payment.setTransactionUid(UUID.randomUUID());

        var payload = mapper.toDepositRequestedPayload(payment);

        assertEquals(payment.getTransactionUid().toString(), payload.getTransactionId());
        assertEquals(payment.getWalletUid().toString(), payload.getWalletId());
        assertEquals(payment.getUserUid().toString(), payload.getUserId());
        assertEquals("99.99", payload.getAmount());
        assertNotNull(payload.getTimestamp());
    }

    @Test
    void toWithdrawalRequestedPayload_shouldMapWithCommentAsDestination() {
        var payment = new PaymentRequest();
        payment.setUid(UUID.randomUUID());
        payment.setWalletUid(UUID.randomUUID());
        payment.setUserUid(UUID.randomUUID());
        payment.setAmount(new BigDecimal("20.00"));
        payment.setFee(new BigDecimal("5.00"));
        payment.setComment("Visa **** 1234");
        payment.setTransactionUid(UUID.randomUUID());
        payment.setCurrency(Currency.getInstance("RUB"));
        payment.setTotalAmount(new BigDecimal("25.00"));

        var payload = mapper.toWithdrawalRequestedPayload(payment);

        assertEquals("25.00", payload.getAmount());
        assertEquals("Visa **** 1234", payload.getDestination());
        assertNotNull(payload.getTimestamp());
    }

}