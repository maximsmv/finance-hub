package com.advanced.transactionservice.service.impl;

import com.advanced.transactionservice.service.CalculationFeeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalculationFeeServiceImpl implements CalculationFeeService {

    @Override
    public BigDecimal calculationDepositFee(String currency) {
        //Тут какая-нибудь бизнес логика расчета комиссии
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculationWithdrawalFee() {
        //Тут какая-нибудь бизнес логика расчета комиссии
        return BigDecimal.valueOf(0.15);
    }

    @Override
    public BigDecimal calculationTransferFee() {
        //Тут какая-нибудь бизнес логика расчета комиссии
        return BigDecimal.valueOf(0.10);
    }

}
