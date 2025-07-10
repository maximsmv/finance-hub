package com.advanced.transactionservice.service;


import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.model.Wallet;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletService {
    WalletResponse createWallet(@Valid CreateWalletRequest request);

    WalletResponse getWalletByUid(String walletUid);

    List<WalletResponse> getWalletsByUser(String userUid);

    void transfer(String fromWalletUid, String toWalletUid, BigDecimal debitAmount, BigDecimal creditAmount);
}
