package com.advanced.transactionservice.service.impl;

import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.service.WalletTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletTypeServiceImpl implements WalletTypeService {

    private final WalletTypeRepository walletTypeRepository;

    @Override
    public Optional<WalletType> getWalletType(UUID uuid) {
        return walletTypeRepository.findById(uuid);
    }
}
