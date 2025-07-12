package com.advanced.transactionservice.mapper;

import com.advanced.kafka.contracts.model.DepositRequestedPayload;
import com.advanced.kafka.contracts.model.WithdrawalRequestedPayload;
import com.advanced.transactionservice.model.PaymentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;

@Mapper(componentModel = "spring", imports = OffsetDateTime.class)
public interface KafkaPayloadMapper {

    @Mapping(target = "transactionId", expression = "java(payment.getTransactionUid().toString())")
    @Mapping(target = "walletId", expression = "java(payment.getWalletUid().toString())")
    @Mapping(target = "userId", expression = "java(payment.getUserUid().toString())")
//    @Mapping(target = "currency", source = "currency", qualifiedByName = "currencyToString")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "timestamp", expression = "java(OffsetDateTime.now())")
    DepositRequestedPayload toDepositRequestedPayload(PaymentRequest payment);

    @Mapping(target = "transactionId", expression = "java(payment.getTransactionUid().toString())")
    @Mapping(target = "walletId", expression = "java(payment.getWalletUid().toString())")
    @Mapping(target = "userId", expression = "java(payment.getUserUid().toString())")
//    @Mapping(target = "currency", source = "currency", qualifiedByName = "currencyToString")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "destination", source = "comment")
    @Mapping(target = "timestamp", expression = "java(OffsetDateTime.now())")
    WithdrawalRequestedPayload toWithdrawalRequestedPayload(PaymentRequest payment);

    @Named("currencyToString")
    static String currencyToString(Currency currency) {
        return currency.getCurrencyCode();
    }

    @Named("bigDecimalToString")
    static String bigDecimalToString(BigDecimal amount) {
        return amount.toPlainString();
    }

}
