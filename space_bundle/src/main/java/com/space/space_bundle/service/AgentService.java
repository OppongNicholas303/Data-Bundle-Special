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
    private final AgentCheckerPricingRepository agentCheckerPricingRepository;
    private final ResultCheckerPricingRepository resultCheckerPricingRepository;
    private final UserRepository userRepository;
    private final BundleRepository bundleRepository;
    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransactionService transactionService;
    private final AgentMashupPricingRepository agentMashupPricingRepository;
    private final MashupRepository mashupRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    private final WalletService walletService;

    public void atomicUpdateStats(String agentProfileId, BigDecimal sales, BigDecimal profit) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("id").is(agentProfileId)
        );
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .inc("totalSales", sales)
                .inc("totalProfit", profit)
                .set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, AgentProfile.class);
    }

    @Transactional
    public AgentProfile register(String userId, String businessName) {
        if (agentProfileRepository.existsByUserId(userId))
            throw new IllegalStateException("Agent profile already exists for userId=" + userId);

        // Do NOT grant ROLE_AGENT immediately. Agent registration should be reviewed by admin
        // before the user receives agent privileges. Create a pending agent profile (active=false).
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        AgentProfile profile = agentProfileRepository.save(AgentProfile.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .businessName(businessName)
                .referralCode(generateUniqueCode())
                .totalSales(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                // pending approval by admin
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("[AGENT] Registered: userId={}, code={}", userId, profile.getReferralCode());
        return profile;
    }

    public AgentProfile getProfileByUserId(String userId) {
        return agentProfileRepository.findFirstByUserId(userId)
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

        // Get existing pricing to find this agent's assigned base price
        AgentBundlePricing pricing = agentBundlePricingRepository
                .findByAgentIdAndBundleId(profile.getId(), bundleId)
                .orElse(AgentBundlePricing.builder()
                        .id(UUID.randomUUID().toString())
                        .agentId(profile.getId())
                        .bundleId(bundleId)
                        // Default base price is the platform selling price
                        // (admin can override this per agent)
                        .basePrice(bundle.getSellingPrice())
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        // Agent's selling price must be >= their assigned base price
        BigDecimal floor = pricing.getBasePrice() != null
                ? pricing.getBasePrice()
                : bundle.getSellingPrice();

        if (sellingPrice.compareTo(floor) < 0)
            throw new IllegalArgumentException(
                    "Your selling price GHS " + sellingPrice
                    + " cannot be below your assigned base price GHS " + floor);

        pricing.setSellingPrice(sellingPrice);
        // Sync base price in case platform bundle price changed
        if (pricing.getBasePrice() == null)
            pricing.setBasePrice(floor);
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

    /**
     * Returns the agent's assigned base price for a bundle.
     * This is what the platform earns from the agent per sale.
     * Falls back to the provided platform default if no custom base price is set.
     */
    public BigDecimal resolveAgentBasePrice(String agentUserId, String bundleId, BigDecimal platformDefault) {
        AgentProfile profile = getProfileByUserId(agentUserId);
        return agentBundlePricingRepository
                .findByAgentIdAndBundleId(profile.getId(), bundleId)
                .filter(AgentBundlePricing::isActive)
                .map(AgentBundlePricing::getBasePrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .orElse(platformDefault);
    }

    public List<AgentBundlePricing> getPricings(String agentUserId) {
        return agentBundlePricingRepository.findByAgentId(getProfileByUserId(agentUserId).getId());
    }

    // ── Admin: per-agent base price management ─────────────────────────────

    /**
     * Admin assigns a custom base price for a specific agent on a specific bundle.
     * basePrice = what the agent pays the platform per sale.
     * Agent can then sell at any price >= basePrice to earn their own profit.
     * Different agents can have different base prices for the same bundle.
     */
    @Transactional
    public AgentBundlePricing adminSetAgentBasePrice(String agentProfileId, String bundleId,
                                                      BigDecimal basePrice, BigDecimal sellingPrice) {
        AgentProfile profile = agentProfileRepository.findById(agentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentProfileId));

        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));

        // Platform must never lose money
        if (basePrice.compareTo(bundle.getCostPrice()) < 0)
            throw new IllegalArgumentException(
                    "Base price " + basePrice + " is below platform cost price " + bundle.getCostPrice());

        AgentBundlePricing pricing = agentBundlePricingRepository
                .findByAgentIdAndBundleId(agentProfileId, bundleId)
                .orElse(AgentBundlePricing.builder()
                        .id(UUID.randomUUID().toString())
                        .agentId(agentProfileId)
                        .bundleId(bundleId)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        pricing.setBasePrice(basePrice);

        // Determine selling price
        if (sellingPrice != null) {
            if (sellingPrice.compareTo(basePrice) < 0)
                throw new IllegalArgumentException(
                        "Selling price cannot be below base price " + basePrice);
            pricing.setSellingPrice(sellingPrice);
        } else if (pricing.getSellingPrice() == null
                || pricing.getSellingPrice().compareTo(basePrice) < 0) {
            // If existing selling price is now below new base, reset it to base
            pricing.setSellingPrice(basePrice);
        }

        pricing.setUpdatedAt(LocalDateTime.now());
        AgentBundlePricing saved = agentBundlePricingRepository.save(pricing);
        log.info("[ADMIN] Agent base price set: agentId={}, bundleId={}, base={}, selling={}",
                agentProfileId, bundleId, basePrice, saved.getSellingPrice());
        return saved;
    }

    /** Admin bulk-applies the same base price to ALL agents for a bundle. */
    @Transactional
    public int adminSetBasePriceForAllAgents(String bundleId, BigDecimal basePrice) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));
        if (basePrice.compareTo(bundle.getCostPrice()) < 0)
            throw new IllegalArgumentException("Base price cannot be below platform cost price");

        List<AgentProfile> allAgents = agentProfileRepository.findAll();
        int updated = 0;
        for (AgentProfile agent : allAgents) {
            try {
                adminSetAgentBasePrice(agent.getId(), bundleId, basePrice, null);
                updated++;
            } catch (Exception e) {
                log.warn("[ADMIN] Could not update agentId={}: {}", agent.getId(), e.getMessage());
            }
        }
        log.info("[ADMIN] Bulk update: bundleId={}, base={}, updated={}", bundleId, basePrice, updated);
        return updated;
    }

    /** Admin view: all agent pricings for one bundle. */
    public List<AgentBundlePricing> adminGetPricingsForBundle(String bundleId) {
        return agentBundlePricingRepository.findByBundleId(bundleId);
    }

    /** Admin view: all pricings for one agent. */
    public List<AgentBundlePricing> adminGetPricingsForAgent(String agentProfileId) {
        return agentBundlePricingRepository.findByAgentId(agentProfileId);
    }

    // ── Checker Pricing Management ─────────────────────────────────────────

    public AgentCheckerPricing adminSetAgentCheckerPrice(String agentProfileId, String serviceName,
                                                         BigDecimal basePrice, BigDecimal sellingPrice) {
        AgentProfile profile = agentProfileRepository.findById(agentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        ResultCheckerPricing globalPricing = resultCheckerPricingRepository.findById(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Checker service not found: " + serviceName));

        // Use global retail price as fallback if basePrice isn't provided
        BigDecimal newBase = basePrice != null ? basePrice : globalPricing.getRetailPrice();
        if (newBase == null) {
            throw new IllegalArgumentException("Global Retail Price is not set. Please set it first.");
        }

        // Base price cannot be below the global retail price OR amount (cost)
        if (newBase.compareTo(globalPricing.getAmount()) < 0) {
            throw new IllegalArgumentException("Agent base price cannot be below platform cost price");
        }

        AgentCheckerPricing pricing = agentCheckerPricingRepository.findByAgentIdAndServiceName(agentProfileId, serviceName)
                .orElse(AgentCheckerPricing.builder()
                        .id(UUID.randomUUID().toString())
                        .agentId(agentProfileId)
                        .serviceName(serviceName)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        pricing.setBasePrice(newBase);

        if (sellingPrice != null) {
            if (sellingPrice.compareTo(pricing.getBasePrice()) < 0) {
                throw new IllegalArgumentException("Selling price cannot be below base price");
            }
            pricing.setSellingPrice(sellingPrice);
        } else if (pricing.getSellingPrice() == null || pricing.getSellingPrice().compareTo(pricing.getBasePrice()) < 0) {
            // Default selling price
            pricing.setSellingPrice(pricing.getBasePrice());
        }

        pricing.setUpdatedAt(LocalDateTime.now());
        return agentCheckerPricingRepository.save(pricing);
    }

    @Transactional
    public int adminSetCheckerBasePriceForAllAgents(String serviceName, BigDecimal basePrice) {
        ResultCheckerPricing globalPricing = resultCheckerPricingRepository.findById(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Checker service not found: " + serviceName));

        if (basePrice.compareTo(globalPricing.getAmount()) < 0)
            throw new IllegalArgumentException("Base price cannot be below platform cost price");

        List<AgentProfile> allAgents = agentProfileRepository.findAll();
        int updated = 0;
        for (AgentProfile agent : allAgents) {
            try {
                adminSetAgentCheckerPrice(agent.getId(), serviceName, basePrice, null);
                updated++;
            } catch (Exception e) {
                log.warn("[ADMIN] Could not update checker pricing agentId={}: {}", agent.getId(), e.getMessage());
            }
        }
        return updated;
    }

    public List<AgentCheckerPricing> adminGetCheckerPricingsForAgent(String agentProfileId) {
        return agentCheckerPricingRepository.findByAgentId(agentProfileId);
    }

    public List<AgentCheckerPricing> adminGetCheckerPricingsForService(String serviceName) {
        return agentCheckerPricingRepository.findByServiceName(serviceName);
    }

    public List<AgentCheckerPricing> getAgentCheckerPricingsByUserId(String userId) {
        AgentProfile profile = getProfileByUserId(userId);
        return agentCheckerPricingRepository.findByAgentId(profile.getId());
    }

    public AgentCheckerPricing setAgentCheckerSellingPrice(String userId, String serviceName, BigDecimal sellingPrice) {
        AgentProfile profile = getProfileByUserId(userId);

        ResultCheckerPricing globalPricing = resultCheckerPricingRepository.findById(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Checker service not found: " + serviceName));

        AgentCheckerPricing pricing = agentCheckerPricingRepository.findByAgentIdAndServiceName(profile.getId(), serviceName)
                .orElse(AgentCheckerPricing.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .agentId(profile.getId())
                        .serviceName(serviceName)
                        .basePrice(globalPricing.getRetailPrice() != null ? globalPricing.getRetailPrice() : globalPricing.getAmount())
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        BigDecimal floor = pricing.getBasePrice() != null ? pricing.getBasePrice() : (globalPricing.getRetailPrice() != null ? globalPricing.getRetailPrice() : globalPricing.getAmount());

        if (sellingPrice.compareTo(floor) < 0) {
            throw new IllegalArgumentException("Selling price cannot be lower than base price: " + floor);
        }

        pricing.setSellingPrice(sellingPrice);
        pricing.setUpdatedAt(LocalDateTime.now());
        return agentCheckerPricingRepository.save(pricing);
    }

    @Transactional
    public AgentMashupPricing adminSetAgentMashupPrice(String agentProfileId, String mashupBundleId,
                                                       BigDecimal basePrice, BigDecimal sellingPrice) {
        AgentProfile profile = agentProfileRepository.findById(agentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentProfileId));

        MashupBundle bundle = mashupRepository.findById(mashupBundleId)
                .orElseThrow(() -> new IllegalArgumentException("Mashup bundle not found: " + mashupBundleId));

        if (!bundle.isPurchasable())
            throw new IllegalStateException("Mashup bundle is not active/purchasable");

        BigDecimal floor = bundle.getSellingPrice();
        BigDecimal effectiveBasePrice = basePrice != null ? basePrice : floor;
        if (effectiveBasePrice.compareTo(floor) < 0)
            throw new IllegalArgumentException("Base price cannot be below Mashup selling price " + floor);

        BigDecimal effectiveSellingPrice = sellingPrice != null ? sellingPrice : effectiveBasePrice;
        if (effectiveSellingPrice.compareTo(effectiveBasePrice) < 0)
            throw new IllegalArgumentException("Selling price cannot be below base price " + effectiveBasePrice);

        AgentMashupPricing pricing = agentMashupPricingRepository
                .findByAgentIdAndMashupBundleId(profile.getId(), mashupBundleId)
                .orElse(AgentMashupPricing.builder()
                        .id(UUID.randomUUID().toString())
                        .agentId(profile.getId())
                        .mashupBundleId(mashupBundleId)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        pricing.setBasePrice(effectiveBasePrice);
        pricing.setSellingPrice(effectiveSellingPrice);
        pricing.setUpdatedAt(LocalDateTime.now());
        return agentMashupPricingRepository.save(pricing);
    }

    @Transactional
    public int adminSetMashupPriceForAllAgents(String mashupBundleId, BigDecimal basePrice) {
        MashupBundle bundle = mashupRepository.findById(mashupBundleId)
                .orElseThrow(() -> new IllegalArgumentException("Mashup bundle not found: " + mashupBundleId));
        if (!bundle.isPurchasable())
            throw new IllegalStateException("Mashup bundle is not active/purchasable");
        if (basePrice.compareTo(bundle.getSellingPrice()) < 0)
            throw new IllegalArgumentException("Base price cannot be below Mashup selling price");

        int updated = 0;
        for (AgentProfile agent : agentProfileRepository.findAll()) {
            try {
                adminSetAgentMashupPrice(agent.getId(), mashupBundleId, basePrice, null);
                updated++;
            } catch (Exception e) {
                log.warn("[ADMIN] Could not update Mashup price for agentId={}: {}", agent.getId(), e.getMessage());
            }
        }
        return updated;
    }

    public List<AgentMashupPricing> adminGetMashupPricingsForAgent(String agentProfileId) {
        return agentMashupPricingRepository.findByAgentId(agentProfileId);
    }

    public List<AgentMashupPricing> adminGetMashupPricingsForBundle(String mashupBundleId) {
        return agentMashupPricingRepository.findByMashupBundleId(mashupBundleId);
    }

    @Transactional
    public AgentMashupPricing setMashupPrice(String agentUserId, String mashupBundleId, BigDecimal sellingPrice) {
        AgentProfile profile = getProfileByUserId(agentUserId);
        MashupBundle bundle = mashupRepository.findById(mashupBundleId)
                .orElseThrow(() -> new IllegalArgumentException("Mashup bundle not found: " + mashupBundleId));

        if (!bundle.isPurchasable())
            throw new IllegalStateException("Mashup bundle is not active/purchasable");

        if (sellingPrice.compareTo(bundle.getSellingPrice()) < 0)
            throw new IllegalArgumentException(
                    "Selling price " + sellingPrice + " cannot be below base price " + bundle.getSellingPrice());

        AgentMashupPricing pricing = agentMashupPricingRepository
                .findByAgentIdAndMashupBundleId(profile.getId(), mashupBundleId)
                .orElse(AgentMashupPricing.builder()
                        .id(UUID.randomUUID().toString())
                        .agentId(profile.getId())
                        .mashupBundleId(mashupBundleId)
                        .basePrice(bundle.getSellingPrice())
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());

        pricing.setSellingPrice(sellingPrice);
        pricing.setBasePrice(bundle.getSellingPrice()); // sync in case platform changed it
        pricing.setUpdatedAt(LocalDateTime.now());
        return agentMashupPricingRepository.save(pricing);
    }

    public BigDecimal resolveEffectiveMashupPrice(String agentUserId, String mashupBundleId) {
        AgentProfile profile = getProfileByUserId(agentUserId);
        return agentMashupPricingRepository
                .findByAgentIdAndMashupBundleId(profile.getId(), mashupBundleId)
                .filter(AgentMashupPricing::isActive)
                .map(AgentMashupPricing::getSellingPrice)
                .orElseGet(() -> mashupRepository.findById(mashupBundleId)
                        .orElseThrow(() -> new IllegalArgumentException("Mashup bundle not found: " + mashupBundleId))
                        .getSellingPrice());
    }

    public List<AgentMashupPricing> getMashupPricings(String agentUserId) {
        return agentMashupPricingRepository.findByAgentId(getProfileByUserId(agentUserId).getId());
    }

    public List<AgentStorefrontBundle> getStorefront(String referralCode) {
        AgentProfile profile = resolveByCode(referralCode);
        return getStorefrontByProfile(profile, referralCode);
    }

    public List<AgentStorefrontBundle> getStorefrontByProfile(AgentProfile profile, String referralCode) {
        List<AgentStorefrontBundle> standardBundles = bundleRepository.findAll().stream()
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
                            .agentCode(referralCode)
                            .bundleType("STANDARD")
                            .build();
                }).toList();

        List<AgentStorefrontBundle> mashupBundles = mashupRepository.findAll().stream()
                .filter(MashupBundle::isPurchasable)
                .map(bundle -> {
                    BigDecimal price = agentMashupPricingRepository
                            .findByAgentIdAndMashupBundleId(profile.getId(), bundle.getId())
                            .filter(AgentMashupPricing::isActive)
                            .map(AgentMashupPricing::getSellingPrice)
                            .orElse(bundle.getSellingPrice());
                    return AgentStorefrontBundle.builder()
                            .bundleId(String.valueOf(bundle.getSpecialOfferPackageId())).bundleCode(bundle.getSlug())
                            .name(bundle.getName()).dataSize(bundle.getDataSize() != null ? bundle.getDataSize() : bundle.getDataAmountMb() + "MB")
                            .network(bundle.getNetwork()).sellingPrice(price)
                            .agentCode(referralCode)
                            .bundleType("MASHUP")
                            .build();
                }).toList();

        java.util.List<AgentStorefrontBundle> allBundles = new java.util.ArrayList<>();
        allBundles.addAll(standardBundles);
        allBundles.addAll(mashupBundles);
        return allBundles;
    }

    @Transactional
    public WithdrawalRequest initiateWithdrawal(String agentUserId, BigDecimal amount,
                                                 String momoProvider, String momoNumber, String accountName) {
        if (amount.compareTo(new BigDecimal("10")) < 0)
            throw new IllegalArgumentException("Minimum withdrawal is GHS 10.00");

        AgentProfile profile = getProfileByUserId(agentUserId);

        Wallet wallet = walletRepository.findFirstByUserId(agentUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        if (wallet.getCommissionBalance().compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient commission balance. Available: GHS " + wallet.getCommissionBalance());

        String reference = "WD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);

        // Deduct from commission wallet immediately so agent cannot double-request
        BigDecimal before = wallet.getCommissionBalance();
        BigDecimal after = walletService.atomicCommissionDebit(agentUserId, amount);
        if (after == null) {
            throw new IllegalStateException("Insufficient balance or concurrent update");
        }

        transactionService.createWithdrawal(agentUserId, reference, amount, before,
                after, "Withdrawal request pending admin approval — " + momoNumber + " (" + momoProvider + ")");

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
