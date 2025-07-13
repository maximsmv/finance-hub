package com.advanced.transactionservice.service.validation;

import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.exception.TransferSameWalletsException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class TransactionValidation {
    public void validateDeposit(WalletResponse wallet) {
        //Какая-нибудь дополнительная проверка
        WalletValidation.validateDeposit(wallet);
    }

    public void validateWithdrawal(WalletResponse wallet, BigDecimal totalAmount) {
        //Какая-нибудь дополнительная проверка
        WalletValidation.validateWithdrawal(wallet, totalAmount);
    }

    public void validateTransfer(WalletResponse fromWallet, WalletResponse toWallet, BigDecimal totalAmount) {
        //Какая-нибудь дополнительная проверка
        WalletValidation.validateTransfer(fromWallet, toWallet, totalAmount);
    }
}
