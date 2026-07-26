package com.space.space_bundle.service;

import com.space.space_bundle.dto.TopUpResponse;
import com.space.space_bundle.entity.Transaction;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.feature.FeatureFlagService;
import com.space.space_bundle.repository.UserRepository;
import com.space.space_bundle.repository.WalletRepository;
import com.space.space_bundle.security.MoolreAdapter;
import com.space.space_bundle.security.PaystackAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final MoolreAdapter moolreAdapter;
    private final PaystackAdapter paystackAdapter;
    private final UserRepository userRepository;
    private final FeatureFlagService featureFlagService;
    private final MongoTemplate mongoTemplate;

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
        atomicCredit(userId, amount);
        transactionService.createCompletedCredit(userId, wallet.getId(), amount, before, before.add(amount), description);
    }

    public void atomicCredit(String userId, BigDecimal amount) {
        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().inc("balance", amount).set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Wallet.class);
    }

    @Transactional
    public void debit(String userId, BigDecimal amount, String orderId, String description) {
        Wallet wallet = getByUserId(userId);
        BigDecimal before = wallet.getBalance();
        if (before.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        
        boolean success = atomicDebit(userId, amount);
        if (!success) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        
        transactionService.createCompletedDebit(userId, orderId, amount, before, before.subtract(amount), description);
    }

    public boolean atomicDebit(String userId, BigDecimal amount) {
        Query query = new Query(Criteria.where("userId").is(userId).and("balance").gte(amount));
        Update update = new Update().inc("balance", amount.negate()).set("updatedAt", LocalDateTime.now());
        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, Wallet.class);
        return result.getModifiedCount() > 0;
    }

    public BigDecimal getBalance(String userId) {
        return getByUserId(userId).getBalance();
    }

    public Wallet getByUserId(String userId) {
        return walletRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for userId=" + userId));
    }

    @Transactional
    public TopUpResponse initializeTopUp(String userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ONE) < 0)
            throw new IllegalArgumentException("Minimum top-up amount is GHS 1.00");

        boolean paystackEnabled = featureFlagService.isEnabled("payment.paystack.enabled", false);
        boolean moolreEnabled = featureFlagService.isEnabled("payment.moolre.enabled", true);

        if (!paystackEnabled && !moolreEnabled) {
            throw new IllegalStateException("External checkout is disabled. Top-ups are currently unavailable.");
        }

        String topUpId = UUID.randomUUID().toString();
        String reference = "TOPUP_" + topUpId;

        Wallet wallet = getByUserId(userId);
        transactionService.createCredit(userId, topUpId, amount,
                wallet.getBalance(), wallet.getBalance().add(amount), "Wallet top-up pending");

        String email = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getEmail();

        String paymentAuthUrl;
        String paymentRef;

        if (moolreEnabled) {
            var responseData = moolreAdapter.generatePaymentLink(
                    amount.doubleValue(), email, reference, moolreCallbackUrl, moolreRedirectUrl);
            paymentRef = (String) responseData.get("reference");
            if (paymentRef == null) paymentRef = reference;
            paymentAuthUrl = (String) responseData.get("authorization_url");
        } else {
            // Apply 2% surcharge for Paystack to cover fees
            BigDecimal chargeMultiplier = BigDecimal.valueOf(1.02);
            int paystackAmountInPesewas = amount.multiply(chargeMultiplier)
                    .multiply(BigDecimal.valueOf(100)).intValue();

            var responseData = paystackAdapter.initializeTransaction(
                    email, paystackAmountInPesewas, reference, moolreRedirectUrl);
            if (!responseData.isStatus() || responseData.getData() == null) {
                throw new RuntimeException("Paystack init failed: " + responseData.getMessage());
            }
            paymentRef = responseData.getData().getAccess_code();
            paymentAuthUrl = responseData.getData().getAuthorization_url();
        }

        return TopUpResponse.builder()
                .topUpId(topUpId)
                .authorizationUrl(paymentAuthUrl)
                .reference(reference)
                .accessCode(paymentRef)
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
        BigDecimal actualCreditAmount = pending.getAmount(); // Use original requested amount
        
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(actualCreditAmount);
        
        // Atomically claim the transaction first to prevent race conditions
        boolean claimed = transactionService.atomicComplete(pending.getId(), before, after);
        if (!claimed) {
            log.warn("TopUp transaction {} was already claimed by another thread", topUpId);
            return; // Another thread already processed it!
        }
        
        atomicCredit(pending.getUserId(), actualCreditAmount);
        
        log.info("Wallet topped up: userId={}, amount={}", pending.getUserId(), actualCreditAmount);
    }
}
