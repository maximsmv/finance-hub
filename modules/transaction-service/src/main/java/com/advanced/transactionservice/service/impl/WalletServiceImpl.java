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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        WalletType walletType = walletTypeService.getWalletType(request.getWalletTypeUid())
                .orElseThrow(() -> new IllegalArgumentException("Wallet type not found"));

        Wallet wallet = walletMapper.toEntity(request);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setWalletType(walletType);

        Wallet saved = walletRepository.save(wallet);
        return walletMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUid(UUID walletUid) {
        Wallet wallet = walletRepository.findById(walletUid)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUid(UUID walletUid, UUID userUid) {
        Wallet wallet = walletRepository.findByUidAndUserUid(walletUid, userUid)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUser(UUID userUid) {
        return Optional.ofNullable(walletRepository.findByUserUid(userUid))
                .orElse(Collections.emptyList())
                .stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public synchronized void transfer(
            UUID fromWalletUid,
            UUID fromUserUid,
            UUID toWalletUid,
            UUID toUserUid,
            BigDecimal debitAmount,
            BigDecimal creditAmount
    ) {
        Wallet from = walletRepository.findForUpdate(fromWalletUid, fromUserUid)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        Wallet to = walletRepository.findForUpdate(toWalletUid, toUserUid)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        debit(from, debitAmount);
        credit(to, creditAmount);
    }

    @Override
    @Transactional
    public synchronized void credit(UUID walletUid, UUID userUid, BigDecimal creditAmount) {
        Wallet wallet = walletRepository.findForUpdate(walletUid, userUid)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        credit(wallet, creditAmount);
    }

    @Override
    @Transactional
    public synchronized void debit(UUID walletUid, UUID userUid, BigDecimal debitAmount) {
        Wallet wallet = walletRepository.findForUpdate(walletUid, userUid)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        debit(wallet, debitAmount);
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
