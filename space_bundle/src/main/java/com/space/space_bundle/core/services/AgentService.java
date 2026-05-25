package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.*;
import com.space.space_bundle.core.port.out.*;
import com.space.space_bundle.core.port.out.authenticationPort.UserRepositoryPort;
import com.space.space_bundle.out.payment.PaystackAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private final AgentProfileRepositoryPort agentProfileRepository;
    private final AgentBundlePricingRepositoryPort agentBundlePricingRepository;
    private final UserRepositoryPort userRepository;
    private final BundleRepositoryPort bundleRepository;
    private final WalletRepositoryPort walletRepository;
    private final TransactionService transactionService;
    private final PaystackAdapter paystackAdapter;

    // ─── Registration ────────────────────────────────────────────────────────

    @Transactional
    public AgentProfile registerAgent(String userId, String businessName) {
        if (agentProfileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Agent profile already exists for userId=" + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Elevate role to ROLE_AGENT
        user.getRoles().add(User.Role.ROLE_AGENT);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        AgentProfile profile = AgentProfile.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .businessName(businessName)
                .referralCode(generateUniqueReferralCode())
                .totalSales(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("[AGENT] Registered agent: userId={}, businessName={}, referralCode={}",
                userId, businessName, profile.getReferralCode());
        return agentProfileRepository.save(profile);
    }

    // ─── Profile ─────────────────────────────────────────────────────────────

    public AgentProfile getProfile(String userId) {
        return agentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Agent profile not found for userId=" + userId));
    }

    // ─── Bundle Pricing ───────────────────────────────────────────────────────

    /**
     * Returns all platform bundles enriched with agent's custom price (if set).
     */
    public List<Bundle> getBundlesForAgent(String agentUserId) {
        return bundleRepository.findAll().stream()
                .filter(b -> b.getStatus() == Bundle.BundleStatus.ACTIVE)
                .toList();
    }

    public List<AgentBundlePricing> getAgentPricings(String agentUserId) {
        AgentProfile profile = getProfile(agentUserId);
        return agentBundlePricingRepository.findByAgentId(profile.getId());
    }

    /**
     * Set or update agent's custom selling price for a bundle.
     * Enforces: sellingPrice >= bundle.sellingPrice (base price)
     */
    @Transactional
    public AgentBundlePricing setOrUpdateBundlePrice(String agentUserId, String bundleId, BigDecimal sellingPrice) {
        AgentProfile profile = getProfile(agentUserId);

        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));

        if (bundle.getStatus() != Bundle.BundleStatus.ACTIVE) {
            throw new IllegalStateException("Bundle is not active: " + bundleId);
        }

        BigDecimal basePrice = bundle.getSellingPrice();

        if (sellingPrice.compareTo(basePrice) < 0) {
            throw new IllegalArgumentException(
                    "Selling price " + sellingPrice + " cannot be lower than base price " + basePrice);
        }

        AgentBundlePricing pricing = agentBundlePricingRepository
                .findByAgentIdAndBundleId(profile.getId(), bundleId)
                .orElse(null);

        if (pricing == null) {
            pricing = AgentBundlePricing.builder()
                    .id(UUID.randomUUID().toString())
                    .agentId(profile.getId())
                    .bundleId(bundleId)
                    .basePrice(basePrice)
                    .sellingPrice(sellingPrice)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            pricing.updateSellingPrice(sellingPrice);
            // Sync basePrice in case platform updated it
            pricing.setBasePrice(basePrice);
        }

        log.info("[AGENT] Pricing set: agentId={}, bundleId={}, sellingPrice={}", profile.getId(), bundleId, sellingPrice);
        return agentBundlePricingRepository.save(pricing);
    }

    /**
     * Resolve the effective price for a bundle when ordered through an agent.
     * Returns agent's sellingPrice if set, otherwise falls back to bundle.sellingPrice.
     */
    public BigDecimal resolveEffectivePrice(String agentUserId, String bundleId) {
        AgentProfile profile = getProfile(agentUserId);
        return agentBundlePricingRepository
                .findByAgentIdAndBundleId(profile.getId(), bundleId)
                .filter(AgentBundlePricing::isActive)
                .map(AgentBundlePricing::getSellingPrice)
                .orElseGet(() -> bundleRepository.findById(bundleId)
                        .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId))
                        .getSellingPrice());
    }

    /**
     * Returns the AgentBundlePricing for a given agent + bundle, or null if not customized.
     */
    public AgentBundlePricing getPricingForBundle(String agentProfileId, String bundleId) {
        return agentBundlePricingRepository
                .findByAgentIdAndBundleId(agentProfileId, bundleId)
                .orElse(null);
    }

    // ─── Withdrawal via Paystack ──────────────────────────────────────────────

    /**
     * Initiate a Paystack transfer to the agent's bank account.
     * Debits agent wallet and creates a WITHDRAWAL transaction.
     */
    @Transactional
    public Map<String, Object> initiateWithdrawal(String agentUserId, BigDecimal amount,
                                                   String bankCode, String accountNumber,
                                                   String accountName) {
        if (amount.compareTo(new BigDecimal("10")) < 0) {
            throw new IllegalArgumentException("Minimum withdrawal amount is GHS 10.00");
        }

        Wallet wallet = walletRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for userId=" + agentUserId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance for withdrawal");
        }

        String reference = "WD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 1. Create recipient on Paystack
        String recipientCode = paystackAdapter.createTransferRecipient(accountName, accountCode(bankCode), accountNumber);

        // 2. Debit wallet first (optimistic debit)
        BigDecimal before = wallet.getBalance();
        wallet.debit(amount);
        walletRepository.save(wallet);
        BigDecimal after = wallet.getBalance();

        // 3. Ledger entry
        transactionService.createWithdrawalTransaction(agentUserId, reference, amount, before, after,
                "Withdrawal to " + accountNumber);

        // 4. Initiate Paystack transfer
        Map<String, Object> transferResult = paystackAdapter.initiateTransfer(
                amount.multiply(BigDecimal.valueOf(100)).intValue(),
                recipientCode,
                reference,
                "Agent withdrawal - " + agentUserId);

        log.info("[AGENT] Withdrawal initiated: userId={}, amount={}, reference={}", agentUserId, amount, reference);
        return transferResult;
    }

    private String accountCode(String bankCode) {
        // Paystack uses bank codes directly for GHS transfers
        return bankCode;
    }

    // ─── Storefront (Public) ──────────────────────────────────────────────────

    /**
     * Resolve an agent profile from a referral code.
     * Used by the public storefront endpoint — no auth required.
     */
    public AgentProfile resolveByReferralCode(String referralCode) {
        return agentProfileRepository.findByReferralCode(referralCode.toUpperCase())
                .filter(AgentProfile::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive agent link: " + referralCode));
    }

    /**
     * Returns all active bundles with the agent's custom prices applied.
     * This is what the customer sees on the agent's storefront.
     */
    public List<AgentStorefrontBundle> getStorefrontBundles(String referralCode) {
        AgentProfile profile = resolveByReferralCode(referralCode);

        return bundleRepository.findAll().stream()
                .filter(b -> b.getStatus() == Bundle.BundleStatus.ACTIVE)
                .map(bundle -> {
                    BigDecimal effectivePrice = agentBundlePricingRepository
                            .findByAgentIdAndBundleId(profile.getId(), bundle.getId())
                            .filter(AgentBundlePricing::isActive)
                            .map(AgentBundlePricing::getSellingPrice)
                            .orElse(bundle.getSellingPrice());

                    return AgentStorefrontBundle.builder()
                            .bundleId(bundle.getId())
                            .bundleCode(bundle.getCode())
                            .name(bundle.getName())
                            .dataSize(bundle.getDataSize())
                            .network(bundle.getNetwork())
                            .sellingPrice(effectivePrice)
                            .agentCode(referralCode)
                            .build();
                })
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateUniqueReferralCode() {
        String code;
        int attempts = 0;
        do {
            // Format: AGT-XXXXX  (uppercase alphanumeric, 5 chars)
            String random = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 5)
                    .toUpperCase();
            code = "AGT-" + random;
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Failed to generate unique referral code after 10 attempts");
            }
        } while (agentProfileRepository.existsByReferralCode(code));
        return code;
    }
}
