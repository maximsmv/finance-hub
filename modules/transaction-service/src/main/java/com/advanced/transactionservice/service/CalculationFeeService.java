package com.advanced.transactionservice.service;

import com.advanced.transactionservice.model.Currency;

import java.math.BigDecimal;

public interface CalculationFeeService {

    BigDecimal calculationDepositFee(Currency currency);

    BigDecimal calculationWithdrawalFee();

    BigDecimal calculationTransferFee();

}
