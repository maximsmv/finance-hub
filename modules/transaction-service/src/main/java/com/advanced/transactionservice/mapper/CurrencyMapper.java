package com.advanced.transactionservice.mapper;

import com.advanced.transactionservice.model.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    Currency map(com.advanced.kafka.contracts.model.Currency status);

    com.advanced.kafka.contracts.model.Currency map(Currency status);

}
