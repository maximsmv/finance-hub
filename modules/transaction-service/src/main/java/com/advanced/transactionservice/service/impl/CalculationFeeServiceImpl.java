package com.advanced.transactionservice.service.impl;

import com.advanced.transactionservice.model.Currency;
import com.advanced.transactionservice.service.CalculationFeeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalculationFeeServiceImpl implements CalculationFeeService {

    @Override
    public BigDecimal calculationDepositFee(Currency currency) {
        return switch (currency) {
            case EUR -> BigDecimal.valueOf(0.05);
            case USD -> BigDecimal.valueOf(0.1);
            case RUB -> BigDecimal.valueOf(1);
        };
    }

    @Override
    public BigDecimal calculationWithdrawalFee() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculationTransferFee() {
        return BigDecimal.ZERO;
    }

}
