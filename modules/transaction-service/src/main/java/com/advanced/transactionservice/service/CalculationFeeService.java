package com.advanced.transactionservice.service;

import java.math.BigDecimal;

public interface CalculationFeeService {

    BigDecimal calculationDepositFee(String currency);

    BigDecimal calculationWithdrawalFee();

    BigDecimal calculationTransferFee();

}
