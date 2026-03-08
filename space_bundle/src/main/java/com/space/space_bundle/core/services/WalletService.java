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
        BigDecimal before = wallet.getBalance();
        wallet.credit(amount);
        walletRepository.save(wallet);
        BigDecimal after = wallet.getBalance();
        // Create credit transaction
        Transaction creditTx = transactionService.createCreditTransaction(userId, wallet.getId(), amount, before, after, "Wallet top up of " + amount);
    }

    public void debit(String userId, BigDecimal amount, String orderId, String description) {
        Wallet wallet = getWalletByUser(userId);
        BigDecimal before = wallet.getBalance();
        wallet.debit(amount);
        walletRepository.save(wallet);
        BigDecimal after = wallet.getBalance();
        transactionService.createDebitTransaction(userId, orderId, amount, before, after, description);
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
        Wallet wallet = getWalletByUser(userId);
        BigDecimal currentBalance = wallet.getBalance();
        transactionService.createCreditTransaction(
            userId, topUpId, amount, currentBalance, currentBalance.add(amount), "Wallet top-up pending");
        
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
        BigDecimal before = wallet.getBalance();
        wallet.credit(amount);
        walletRepository.save(wallet);
        BigDecimal after = wallet.getBalance();
        
        // Complete the pending transaction with updated balances
        transactionService.completeTransaction(pendingTx.getId(), before, after);
        
        log.info("Wallet topped up: userId={}, amount={}", userId, amount);
    }
}
