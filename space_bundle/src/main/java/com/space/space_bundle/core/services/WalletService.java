package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.WalletRepositoryPort;
import com.space.space_bundle.in.web.dto.TopUpResponse;
import com.space.space_bundle.out.payment.PaystackAdapter;
import com.space.space_bundle.out.payment.dto.PaystackInitializeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepositoryPort walletRepository;
    private final TransactionService transactionService;
    private final PaystackAdapter paystackAdapter;
    private final UserService userService;

    @Value("${paystack.callback-url:http://localhost:3000/payment/callback}")
    private String paystackCallbackUrl;

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

    public TopUpResponse initializeTopUp(String userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Minimum top-up amount is GHS 1.00");
        }
        
        String topUpId = UUID.randomUUID().toString();
        String reference = "TOPUP_" + topUpId;
        
        // Create pending transaction to store userId
        transactionService.createCreditTransaction(
            userId, topUpId, amount, "Wallet top-up pending");
        
        String userEmail = userService.getUserById(userId).getEmail();
        Integer amountInKobo = amount.multiply(BigDecimal.valueOf(100)).intValue();
        
        PaystackInitializeResponse response = paystackAdapter.initializeTransaction(
            userEmail, amountInKobo, reference, paystackCallbackUrl);
        
        if (!response.isStatus() || response.getData() == null) {
            throw new RuntimeException("Failed to initialize top-up: " + response.getMessage());
        }
        
        return TopUpResponse.builder()
            .topUpId(topUpId)
            .reference(reference)
            .authorizationUrl(response.getData().getAuthorization_url())
            .accessCode(response.getData().getAccess_code())
            .build();
    }

    public void processTopUpPaymentById(String topUpId, BigDecimal amount) {
        // Find the pending transaction to get userId
        Transaction pendingTx = transactionService.getTransactionByOrderId(topUpId);
        String userId = pendingTx.getUserId();
        
        Wallet wallet = getWalletByUser(userId);
        wallet.credit(amount);
        walletRepository.save(wallet);
        
        // Complete the pending transaction
        transactionService.completeTransaction(pendingTx.getId());
        
        log.info("Wallet topped up: userId={}, amount={}", userId, amount);
    }
}
