package com.advanced.transactionservice.mapper;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.model.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "walletType", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "balance", expression = "java(java.math.BigDecimal.ZERO)")
    Wallet toEntity(CreateWalletRequest request);

    @Mapping(source = "wallet.walletType.uid", target = "walletTypeUid")
    WalletResponse toResponse(Wallet wallet);

}
