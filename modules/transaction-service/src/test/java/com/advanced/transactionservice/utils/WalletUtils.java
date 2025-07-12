package com.advanced.transactionservice.utils;

import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public class WalletUtils {

    public static WalletType createWalletType(WalletTypeRepository repository) {
        WalletType type = new WalletType();
        type.setCreator("system");
        type.setName("default");
        type.setUserType("default");
        type.setStatus(WalletStatus.ACTIVE);
        type.setCurrencyCode(Currency.getInstance("RUB"));
        repository.save(type);
        return type;
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
        WalletType type = createWalletType(typeRepository);
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
