package com.space.space_bundle.service;

import com.space.space_bundle.dto.AgentStorefrontBundle;
import com.space.space_bundle.entity.*;
import com.space.space_bundle.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentProfileRepository agentProfileRepository;
    private final AgentBundlePricingRepository agentBundlePricingRepository;
    private final UserRepository userRepository;
    private final BundleRepository bundleRepository;
    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransactionService transactionService;

    @Transactional
    public AgentProfile register(String userId, String businessName) {
        if (agentProfileRepository.existsByUserId(userId))
            throw new IllegalStateException("Agent profile already exists for userId=" + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.getRoles().add(User.Role.ROLE_AGENT.name());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        AgentProfile profile = agentProfileRepository.save(AgentProfile.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .businessName(businessName)
                .referralCode(generateUniqueCode())
                .totalSales(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("[AGENT] Registered: userId={}, code={}", userId, profile.getReferralCode());
        return profile;
    }

    public AgentProfile getProfileByUserId(String userId) {
        return agentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Agent profile not found for userId=" + userId));
    }

    public AgentProfile resolveByCode(String referralCode) {
        String upper = referralCode.toUpperCase(Locale.ROOT);
        log.info("[AGENT] resolveByCode input='{}' normalized='{}'", referralCode, upper);
        AgentProfile profile = agentProfileRepository.findByReferralCode(upper)
                .orElseThrow(() -> new IllegalArgumentException("Invalid agent code: " + referralCode));
        if (!profile.isActive())
            throw new IllegalStateException("Agent storefront is not active");
        return profile;
    }

    @Transactional
    public AgentBundlePricing setPrice(String agentUserId, String bundleId, BigDecimal sellingPrice) {
        AgentProfile profile = getProfileByUserId(agentUserId);
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));

        if (!Bundle.BundleStatus.ACTIVE.name().equals(bundle.getStatus()))
            throw new IllegalStateException("Bundle is not active");

        if (sellingPrice.compareTo(bundle.getSellingPrice()) < 0)
            throw new IllegalArgumentException(
                    "Selling price " + sellingPrice + " cannot be below base price " + bundle.getSellingPrice());

        AgentBundlePricing pricing = agentBundlePricingRepository
                .findByAgentIdAndBundleId(profile.getId(), bundleId)
                .orElse(AgentBundlePricing.builder()
                        .id(UUID.randomUUID().toString())
                        .agentId(profile.getId())
                        .bundleId(bundleId)
                        .basePrice(bundle.getSellingPrice())
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        pricing.setSellingPrice(sellingPrice);
        pricing.setBasePrice(bundle.getSellingPrice()); // sync in case platform changed it
        pricing.setUpdatedAt(LocalDateTime.now());
        return agentBundlePricingRepository.save(pricing);
    }

    public BigDecimal resolveEffectivePrice(String agentUserId, String bundleId) {
        AgentProfile profile = getProfileByUserId(agentUserId);
        return agentBundlePricingRepository
                .findByAgentIdAndBundleId(profile.getId(), bundleId)
                .filter(AgentBundlePricing::isActive)
                .map(AgentBundlePricing::getSellingPrice)
                .orElseGet(() -> bundleRepository.findById(bundleId)
                        .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId))
                        .getSellingPrice());
    }

    public List<AgentBundlePricing> getPricings(String agentUserId) {
        return agentBundlePricingRepository.findByAgentId(getProfileByUserId(agentUserId).getId());
    }

    public List<AgentStorefrontBundle> getStorefront(String referralCode) {
        AgentProfile profile = resolveByCode(referralCode);
        return getStorefrontByProfile(profile, referralCode);
    }

    public List<AgentStorefrontBundle> getStorefrontByProfile(AgentProfile profile, String referralCode) {
        return bundleRepository.findAll().stream()
                .filter(b -> Bundle.BundleStatus.ACTIVE.name().equals(b.getStatus()))
                .map(bundle -> {
                    BigDecimal price = agentBundlePricingRepository
                            .findByAgentIdAndBundleId(profile.getId(), bundle.getId())
                            .filter(AgentBundlePricing::isActive)
                            .map(AgentBundlePricing::getSellingPrice)
                            .orElse(bundle.getSellingPrice());
                    return AgentStorefrontBundle.builder()
                            .bundleId(bundle.getId()).bundleCode(bundle.getCode())
                            .name(bundle.getName()).dataSize(bundle.getDataSize())
                            .network(bundle.getNetwork()).sellingPrice(price)
                            .agentCode(referralCode).build();
                }).toList();
    }

    @Transactional
    public WithdrawalRequest initiateWithdrawal(String agentUserId, BigDecimal amount,
                                                 String momoProvider, String momoNumber, String accountName) {
        if (amount.compareTo(new BigDecimal("10")) < 0)
            throw new IllegalArgumentException("Minimum withdrawal is GHS 10.00");

        AgentProfile profile = getProfileByUserId(agentUserId);

        Wallet wallet = walletRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        if (wallet.getBalance().compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient balance. Available: GHS " + wallet.getBalance());

        String reference = "WD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);

        // Deduct from wallet immediately so agent cannot double-request
        BigDecimal before = wallet.getBalance();
        wallet.debit(amount);
        walletRepository.save(wallet);
        transactionService.createWithdrawal(agentUserId, reference, amount, before,
                wallet.getBalance(), "Withdrawal request pending admin approval — " + momoNumber + " (" + momoProvider + ")");

        WithdrawalRequest request = withdrawalRepository.save(WithdrawalRequest.builder()
                .id(UUID.randomUUID().toString())
                .agentUserId(agentUserId)
                .agentProfileId(profile.getId())
                .amount(amount)
                .momoProvider(momoProvider)
                .momoNumber(momoNumber)
                .accountName(accountName)
                .reference(reference)
                .status(WithdrawalRequest.Status.PENDING.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("[AGENT] Withdrawal request created: userId={}, ref={}, amount={}",
                agentUserId, reference, amount);
        return request;
    }

    public List<WithdrawalRequest> getWithdrawals(String agentUserId) {
        return withdrawalRepository.findByAgentUserIdOrderByCreatedAtDesc(agentUserId);
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = "AGT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase(Locale.ROOT);
            if (++attempts > 10) throw new IllegalStateException("Could not generate unique referral code");
        } while (agentProfileRepository.existsByReferralCode(code));
        return code;
    }
}
