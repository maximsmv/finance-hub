package com.advanced.transactionservice.service;


import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    WalletResponse createWallet(@Valid CreateWalletRequest request);

    WalletResponse getWalletByUid(UUID walletUid);

    List<WalletResponse> getWalletsByUser(UUID userUid);

    void transfer(
            UUID fromWalletUid,
            UUID fromUserUid,
            UUID toWalletUid,
            UUID toUserUid,
            BigDecimal debitAmount,
            BigDecimal creditAmount
    );

    void credit(UUID walletUid, UUID userUid, BigDecimal creditAmount);

    void debit(UUID walletUid, UUID userUid, BigDecimal debitAmount);
}
