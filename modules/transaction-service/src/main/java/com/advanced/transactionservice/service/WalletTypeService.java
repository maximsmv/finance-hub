package com.advanced.transactionservice.service;

import com.advanced.transactionservice.model.WalletType;

import java.util.Optional;
import java.util.UUID;

public interface WalletTypeService {

    Optional<WalletType> getWalletType(UUID uuid);

}
