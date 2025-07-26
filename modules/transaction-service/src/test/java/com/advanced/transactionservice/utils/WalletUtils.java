package com.advanced.transactionservice.utils;

import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletUtils {

    public static final UUID WALLET_TYPE_UID = UUID.fromString("e32bd41e-bb27-4942-adce-f2b406aa5f3e");

    public static WalletType getWalletType(WalletTypeRepository repository) {
        return repository.findById(WALLET_TYPE_UID)
                .orElseThrow(EntityNotFoundException::new);
    }

    public static Wallet createWallet(
            WalletTypeRepository typeRepository,
            WalletRepository walletRepository,
            String name,
            BigDecimal balance
    ) {
        return createWallet(typeRepository, walletRepository, name, balance, WalletStatus.ACTIVE, UUID.randomUUID());
    }

    public static Wallet createWallet(
            WalletTypeRepository typeRepository,
            WalletRepository walletRepository,
            String name,
            BigDecimal balance,
            UUID userUid
    ) {
        return createWallet(typeRepository, walletRepository, name, balance, WalletStatus.ACTIVE, userUid);
    }

    public static Wallet createWallet(
            WalletTypeRepository typeRepository,
            WalletRepository walletRepository,
            String name,
            BigDecimal balance,
            WalletStatus status
    ) {
        return createWallet(typeRepository, walletRepository, name, balance, status, UUID.randomUUID());
    }


    public static Wallet createWallet(
            WalletTypeRepository typeRepository,
            WalletRepository walletRepository,
            String name, BigDecimal balance,
            WalletStatus status,
            UUID userUid
    ) {
        WalletType type = getWalletType(typeRepository);
        Wallet wallet = new Wallet();
        wallet.setName(name);
        wallet.setWalletType(type);
        wallet.setUserUid(userUid);
        wallet.setStatus(status);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
        return wallet;
    }

}
