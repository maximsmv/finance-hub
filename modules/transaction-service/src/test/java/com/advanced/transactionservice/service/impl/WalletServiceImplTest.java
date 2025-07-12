package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.exception.WalletBalanceException;
import com.advanced.transactionservice.mapper.WalletMapper;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.service.WalletTypeService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletMapper walletMapper;
    @Mock
    private WalletTypeService walletTypeService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWallet_shouldSaveWalletAndReturnResponse() {
        UUID walletTypeUid = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest();
        request.setWalletTypeUid(walletTypeUid);

        WalletType walletType = new WalletType();
        Wallet wallet = new Wallet();
        Wallet savedWallet = new Wallet();
        WalletResponse response = new WalletResponse();

        Mockito.when(walletTypeService.getWalletType(walletTypeUid)).thenReturn(Optional.of(walletType));
        Mockito.when(walletMapper.toEntity(request)).thenReturn(wallet);
        Mockito.when(walletRepository.save(wallet)).thenReturn(savedWallet);
        Mockito.when(walletMapper.toResponse(savedWallet)).thenReturn(response);

        WalletResponse result = walletService.createWallet(request);

        assertEquals(response, result);
        Mockito.verify(walletRepository).save(wallet);
    }

    @Test
    void getWalletByUid_shouldReturnWallet() {
        UUID walletUid = UUID.randomUUID();
        Wallet wallet = new Wallet();
        WalletResponse response = new WalletResponse();

        Mockito.when(walletRepository.findById(walletUid)).thenReturn(Optional.of(wallet));
        Mockito.when(walletMapper.toResponse(wallet)).thenReturn(response);

        WalletResponse result = walletService.getWalletByUid(walletUid);
        assertEquals(response, result);
    }

    @Test
    void getWalletByUid_shouldThrowIfNotFound() {
        UUID walletUid = UUID.randomUUID();
        Mockito.when(walletRepository.findById(walletUid)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> walletService.getWalletByUid(walletUid));
    }

    @Test
    void getWalletsByUser_shouldReturnList() {
        UUID userUid = UUID.randomUUID();
        List<Wallet> wallets = List.of(new Wallet(), new Wallet());
        List<WalletResponse> responses = List.of(new WalletResponse(), new WalletResponse());

        Mockito.when(walletRepository.findByUserUid(userUid)).thenReturn(wallets);
        Mockito.when(walletMapper.toResponse(any(Wallet.class))).thenReturn(responses.get(0), responses.get(1));

        List<WalletResponse> result = walletService.getWalletsByUser(userUid);
        assertEquals(2, result.size());
    }

    @Test
    void getWalletsByUser_shouldEmptyListIfNotFound() {
        UUID userUid = UUID.randomUUID();
        Mockito.when(walletRepository.findByUserUid(userUid)).thenReturn(List.of());
        List<WalletResponse> responses = walletService.getWalletsByUser(userUid);
        assertEquals(List.of(), responses);
    }

    @Test
    void transfer_shouldUpdateBalancesSuccessfully() {
        UUID fromUid = UUID.randomUUID();
        UUID toUid = UUID.randomUUID();

        Wallet from = new Wallet();
        from.setUid(fromUid);
        from.setBalance(BigDecimal.valueOf(100));
        from.setStatus(WalletStatus.ACTIVE);

        Wallet to = new Wallet();
        to.setUid(toUid);
        to.setBalance(BigDecimal.valueOf(50));
        to.setStatus(WalletStatus.ACTIVE);

        Mockito.when(walletRepository.findForUpdate(fromUid)).thenReturn(Optional.of(from));
        Mockito.when(walletRepository.findForUpdate(toUid)).thenReturn(Optional.of(to));

        walletService.transfer(fromUid, toUid, BigDecimal.valueOf(30), BigDecimal.valueOf(25));

        assertEquals(new BigDecimal("70.00"), from.getBalance());
        assertEquals(new BigDecimal("75.00"), to.getBalance());
        Mockito.verify(walletRepository).save(from);
        Mockito.verify(walletRepository).save(to);
        Mockito.verify(walletRepository).flush();
    }

    @Test
    void transfer_shouldThrowIfValidationFails() {
        UUID fromUid = UUID.randomUUID();
        UUID toUid = UUID.randomUUID();

        Wallet from = new Wallet();
        from.setUid(fromUid);
        from.setBalance(BigDecimal.valueOf(10));
        from.setStatus(WalletStatus.ACTIVE);

        Wallet to = new Wallet();
        to.setUid(toUid);
        to.setBalance(BigDecimal.valueOf(50));
        to.setStatus(WalletStatus.ACTIVE);

        Mockito.when(walletRepository.findForUpdate(fromUid)).thenReturn(Optional.of(from));
        Mockito.when(walletRepository.findForUpdate(toUid)).thenReturn(Optional.of(to));

        assertThrows(WalletBalanceException.class, () ->
                walletService.transfer(fromUid, toUid, BigDecimal.valueOf(100), BigDecimal.valueOf(95)));
    }

}