package com.space.space_bundle.service;

import com.space.space_bundle.dto.TopUpResponse;
import com.space.space_bundle.entity.Transaction;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.repository.UserRepository;
import com.space.space_bundle.repository.WalletRepository;
import com.space.space_bundle.security.MoolreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final MoolreAdapter moolreAdapter;
    private final UserRepository userRepository;

    @Value("${moolre.callback-url:http://localhost:8080/api/webhook/moolre}")
    private String moolreCallbackUrl;

    @Value("${moolre.redirect-url:http://localhost:3000/payment/callback}")
    private String moolreRedirectUrl;

    @Transactional
    public Wallet createWallet(String userId) {
        return walletRepository.save(Wallet.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency("GHS")
                .status(Wallet.WalletStatus.ACTIVE.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void credit(String userId, BigDecimal amount, String description) {
        Wallet wallet = getByUserId(userId);
        BigDecimal before = wallet.getBalance();
        wallet.credit(amount);
        walletRepository.save(wallet);
        transactionService.createCredit(userId, wallet.getId(), amount, before, wallet.getBalance(), description);
    }

    @Transactional
    public void debit(String userId, BigDecimal amount, String orderId, String description) {
        Wallet wallet = getByUserId(userId);
        BigDecimal before = wallet.getBalance();
        wallet.debit(amount);
        walletRepository.save(wallet);
        transactionService.createDebit(userId, orderId, amount, before, wallet.getBalance(), description);
    }

    public BigDecimal getBalance(String userId) {
        return getByUserId(userId).getBalance();
    }

    public Wallet getByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for userId=" + userId));
    }

    @Transactional
    public TopUpResponse initializeTopUp(String userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ONE) < 0)
            throw new IllegalArgumentException("Minimum top-up amount is GHS 1.00");

        String topUpId = UUID.randomUUID().toString();
        String reference = "TOPUP_" + topUpId;

        Wallet wallet = getByUserId(userId);
        transactionService.createCredit(userId, topUpId, amount,
                wallet.getBalance(), wallet.getBalance().add(amount), "Wallet top-up pending");

        String email = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getEmail();

        var responseData = moolreAdapter.generatePaymentLink(
                amount.doubleValue(), email, reference, moolreCallbackUrl, moolreRedirectUrl);

        String moolreRef = (String) responseData.get("reference");
        if (moolreRef == null) moolreRef = reference;

        return TopUpResponse.builder()
                .topUpId(topUpId)
                .reference(reference)
                .authorizationUrl((String) responseData.get("authorization_url"))
                .accessCode(moolreRef)
                .build();
    }

    @Transactional
    public void processTopUpById(String topUpId, BigDecimal amount) {
        Transaction pending = transactionService.getFirstByOrderId(topUpId);
        
        if (!"PENDING".equalsIgnoreCase(pending.getStatus())) {
            log.warn("TopUp transaction {} is already in status: {}", topUpId, pending.getStatus());
            return;
        }

        Wallet wallet = getByUserId(pending.getUserId());
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        
        // Atomically claim the transaction first to prevent race conditions
        boolean claimed = transactionService.atomicComplete(pending.getId(), before, after);
        if (!claimed) {
            log.warn("TopUp transaction {} was already claimed by another thread", topUpId);
            return; // Another thread already processed it!
        }
        
        wallet.credit(amount);
        walletRepository.save(wallet);
        
        log.info("Wallet topped up: userId={}, amount={}", pending.getUserId(), amount);
    }
}
