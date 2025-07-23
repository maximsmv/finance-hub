package com.advanced.transactionservice.mapper;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.model.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "walletType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "balance", expression = "java(java.math.BigDecimal.ZERO)")
    Wallet toEntity(CreateWalletRequest request);

    @Mapping(target = "currency", source = "wallet.walletType.currencyCode")
    @Mapping(source = "wallet.walletType.uid", target = "walletTypeUid")
    @Mapping(source = "wallet.uid", target = "walletUid")
    WalletResponse toResponse(Wallet wallet);

}
