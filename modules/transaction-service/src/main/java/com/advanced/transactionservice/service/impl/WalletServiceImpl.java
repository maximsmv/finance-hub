package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.mapper.WalletMapper;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.service.WalletService;
import com.advanced.transactionservice.service.WalletTypeService;
import com.advanced.transactionservice.service.validation.WalletValidation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        wallet.setStatus(WalletStatus.ACTIVE);
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

    @Override
    @Transactional
    public synchronized void transfer(String fromWalletUid, String toWalletUid, BigDecimal debitAmount, BigDecimal creditAmount) {
        Wallet from = walletRepository.findForUpdate(UUID.fromString(fromWalletUid))
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        Wallet to = walletRepository.findForUpdate(UUID.fromString(toWalletUid))
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        WalletValidation.validateTransfer(from, to, debitAmount);

        debit(from, debitAmount);
        credit(to, creditAmount);
    }

    private synchronized void debit(Wallet wallet, BigDecimal amount) {
        wallet.setBalance(wallet.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

    }

    private synchronized void credit(Wallet wallet, BigDecimal amount) {
        wallet.setBalance(wallet.getBalance().add(amount).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);
    }
}
