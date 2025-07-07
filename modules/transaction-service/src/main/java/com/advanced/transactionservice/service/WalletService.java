package com.advanced.transactionservice.service;


import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface WalletService {
    WalletResponse createWallet(@Valid CreateWalletRequest request);

    WalletResponse getWalletByUid(String walletUid);

    List<WalletResponse> getWalletsByUser(String userUid);
}
