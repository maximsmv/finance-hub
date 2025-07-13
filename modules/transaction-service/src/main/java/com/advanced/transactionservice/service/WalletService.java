package com.advanced.transactionservice.service;


import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.model.Wallet;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletService {
    WalletResponse createWallet(@Valid CreateWalletRequest request);

    WalletResponse getWalletByUid(UUID walletUid);

    List<WalletResponse> getWalletsByUser(UUID userUid);

    void transfer(UUID fromWalletUid, UUID toWalletUid, BigDecimal debitAmount, BigDecimal creditAmount);

    void deposit(UUID walletUid, BigDecimal creditAmount);

    void withdraw(UUID walletUid, BigDecimal debitAmount);
}
