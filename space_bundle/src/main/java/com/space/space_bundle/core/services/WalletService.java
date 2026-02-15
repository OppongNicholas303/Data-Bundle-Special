package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.WalletRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class WalletService {

    private final WalletRepositoryPort walletRepository;
    private final TransactionService transactionService;

public Wallet createWallet(String userId) {
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency("GHS")
                .status(Wallet.WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return walletRepository.save(wallet);
    }

    public void credit(String userId, BigDecimal amount) {
        Wallet wallet = getWalletByUser(userId);
        wallet.credit(amount);
        walletRepository.save(wallet);
        // Create debit transaction
        Transaction creditTx = transactionService.createCreditTransaction(userId, wallet.getId(), amount, "Wallet top up of " + amount);
    }

    public void debit(String userId, BigDecimal amount) {
        Wallet wallet = getWalletByUser(userId);
        wallet.debit(amount);
        walletRepository.save(wallet);
    }

    public BigDecimal getBalance(String userId) {
        return getWalletByUser(userId).getBalance();
    }

    public Wallet getWalletByUserId(String userId) {
        return getWalletByUser(userId);
    }

    private Wallet getWalletByUser(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
    }
}
