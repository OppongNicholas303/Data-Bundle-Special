package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.PaymentPort;
import com.space.space_bundle.out.persistence.repository.WalletRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class WalletService {

    private final WalletRepositoryPort walletRepository;
    private final PaymentPort paymentPort;

    public void debitForOrder(UUID userId, BigDecimal amount) throws Exception {
        // Debit the real payment provider first
        String txRef = paymentPort.debit(userId, amount);

        // Then debit the wallet
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        wallet.debit(amount);
        walletRepository.save(wallet);

        System.out.println("Debited wallet for order, payment ref: " + txRef);
    }

    public void refundForOrder(UUID userId, BigDecimal amount) throws Exception {
        // Refund the real payment provider first
        String txRef = paymentPort.refund(userId, amount);

        // Then credit the wallet
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        wallet.credit(amount);
        walletRepository.save(wallet);

        System.out.println("Refunded wallet for order, payment ref: " + txRef);
    }

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
    }

    public void debit(String userId, BigDecimal amount) {
        Wallet wallet = getWalletByUser(userId);
        wallet.debit(amount);
        walletRepository.save(wallet);
    }

    public BigDecimal getBalance(String userId) {
        return getWalletByUser(userId).getBalance();
    }

    private Wallet getWalletByUser(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
    }
}
