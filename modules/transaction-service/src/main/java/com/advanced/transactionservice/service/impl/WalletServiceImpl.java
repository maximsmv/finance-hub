package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.mapper.WalletMapper;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.service.WalletService;
import com.advanced.transactionservice.service.WalletTypeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletTypeService walletTypeService;

    private final WalletRepository walletRepository;

    private final WalletMapper walletMapper;

    @Override
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        WalletType walletType = walletTypeService.getWalletType(UUID.fromString(Objects.requireNonNull(request.getWalletTypeUid())))
                .orElseThrow(() -> new IllegalArgumentException("Wallet type not found"));

        Wallet wallet = walletMapper.toEntity(request);
        wallet.setWalletType(walletType);

        Wallet saved = walletRepository.save(wallet);
        return walletMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUid(String walletUid) {
        Wallet wallet = walletRepository.findById(UUID.fromString(walletUid))
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUser(String userUid) {
        return Optional.ofNullable(walletRepository.findByUserUid(UUID.fromString(userUid)))
                .orElse(Collections.emptyList())
                .stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }
}
