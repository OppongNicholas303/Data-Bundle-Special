package com.space.space_bundle.service;

import com.space.space_bundle.dto.SingleOrderUserDTO;
import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.entity.MashupBundle;
import com.space.space_bundle.entity.Order;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.repository.WalletRepository;
import com.space.space_bundle.security.PaystackAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final BundleService bundleService;
    private final MashupService mashupService;
    private final AgentService agentService;
    private final CommissionService commissionService;
    private final TransactionService transactionService;
    private final PaystackAdapter paystackAdapter;
    private final AutomationService automationService;
    private final com.space.space_bundle.feature.FeatureFlagService featureFlagService;
    private final MongoTemplate mongoTemplate;

    @Value("${paystack.callback-url:http://localhost:3000/payment/callback}")
    private String callbackUrl;

    // runtime flag is read from DB via FeatureFlagService (default true)

    private static final Map<String, String> PKG_MTN = Map.ofEntries(
            Map.entry("1GB","20"), Map.entry("2GB","21"), Map.entry("3GB","22"),
            Map.entry("4GB","23"), Map.entry("5GB","24"), Map.entry("6GB","25"),
            Map.entry("8GB","27"), Map.entry("10GB","28"), Map.entry("15GB","29"),
            Map.entry("20GB","30"), Map.entry("25GB","31"));

    private static final Map<String, String> PKG_TELECEL = Map.ofEntries(
            Map.entry("5GB","38"), Map.entry("10GB","39"),
            Map.entry("15GB","40"), Map.entry("20GB","41"));

    // ── Public entry points ────────────────────────────────────────────────

    @Transactional
    public Order placeOrder(String network, String phoneNumber, String bundleCode,
                            String email, String userId, String packageId) {
        return placeOrder(network, phoneNumber, bundleCode, email, userId, packageId, null);
    }

    @Transactional
    public Order placeOrder(String network, String phoneNumber, String bundleCode,
                            String email, String userId, String packageId, String agentCode) {
        return placeOrder(network, phoneNumber, bundleCode, email, userId, packageId, agentCode, null);
    }

    @Transactional
    public Order placeOrder(String network, String phoneNumber, String bundleCode,
                            String email, String userId, String packageId, String agentCode,
                            String bundleType) {

        if (isMashupBundle(bundleType)) {
            return placeMashupOrder(network, phoneNumber, bundleCode, email, userId, packageId, agentCode);
        }

        // Normalize network to lowercase to match DB storage ("mtn", "telecel", "airteltigo")
//        String normalizedNetwork = network == null ? null : network.toLowerCase();

        String normalizedNetwork = network;
        if(network.equals("mtn") ){
            normalizedNetwork = network == null ? null : network.toUpperCase();
        }

        System.out.println(bundleCode + " " + normalizedNetwork);

        // Resolve agent if code provided
        String agentProfileId = null;
        String agentUserId = null;
        BigDecimal baseAmount = bundleService.getPrice(bundleCode, normalizedNetwork);
        BigDecimal costPrice = bundleService.getCostPrice(bundleCode, normalizedNetwork);
        BigDecimal customerAmount = baseAmount;
        BigDecimal commissionAmount = BigDecimal.ZERO;

        System.out.println(baseAmount);

        if (agentCode != null && !agentCode.isBlank()) {
            AgentProfile profile = agentService.resolveByCode(agentCode);
            agentProfileId = profile.getId();
            agentUserId = profile.getUserId();
            customerAmount = agentService.resolveEffectivePrice(agentUserId,
                    bundleService.getByCodeAndNetwork(bundleCode, normalizedNetwork).getId());
            commissionAmount = customerAmount.subtract(baseAmount);
        }

        BigDecimal fee = customerAmount.multiply(BigDecimal.valueOf(0.02));
        BigDecimal total = customerAmount.add(fee);

        String resolvedPkg = resolvePackageId(bundleCode, normalizedNetwork, packageId);

        Order order = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .agentId(agentProfileId)
                .network(normalizedNetwork)
                .phoneNumber(phoneNumber)
                .bundleCode(bundleCode)
                .bundleType("STANDARD")
                .packageId(resolvedPkg)
                .amount(total)
                .baseAmount(baseAmount)
                .costPrice(costPrice)
                .commissionAmount(commissionAmount)
                .status(Order.OrderStatus.CREATED.name())
                .providerStatus("processing")
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Order created: id={}, network={}, base={}, customer={}, commission={}",
                order.getId(), normalizedNetwork, baseAmount, customerAmount, commissionAmount);

        // Try wallet payment first
        if (userId != null) {
            Optional<Wallet> wallet = walletRepository.findByUserId(userId);
            if (wallet.isPresent() && wallet.get().getBalance().compareTo(total) >= 0)
                return processWithWallet(order, wallet.get(), total, userId, email, normalizedNetwork);
        }

        return initPaystack(order, email);
    }

    private Order placeMashupOrder(String network, String phoneNumber, String bundleCode,
                                   String email, String userId, String packageId, String agentCode) {
        MashupBundle mashupBundle = mashupService.getPurchasablePackage(bundleCode, packageId);
        String normalizedNetwork = mashupBundle.getNetwork() != null
                ? mashupBundle.getNetwork().toUpperCase(Locale.ROOT)
                : "MTN";

        if (network != null && !network.isBlank() && !network.equalsIgnoreCase(normalizedNetwork)) {
            throw new IllegalArgumentException("Mashup package is only available on " + normalizedNetwork);
        }

        String agentProfileId = null;
        String agentUserId = null;
        BigDecimal baseAmount = mashupBundle.getSellingPrice();
        BigDecimal costPrice = mashupBundle.getCostPrice() != null
                ? mashupBundle.getCostPrice()
                : BigDecimal.ZERO;
        BigDecimal customerAmount = baseAmount;
        BigDecimal commissionAmount = BigDecimal.ZERO;

        if (agentCode != null && !agentCode.isBlank()) {
            AgentProfile profile = agentService.resolveByCode(agentCode);
            agentProfileId = profile.getId();
            agentUserId = profile.getUserId();
            customerAmount = agentService.resolveEffectiveMashupPrice(agentUserId, mashupBundle.getId());
            commissionAmount = customerAmount.subtract(baseAmount);
        }

        BigDecimal fee = customerAmount.multiply(BigDecimal.valueOf(0.02));
        BigDecimal total = customerAmount.add(fee);
        String resolvedPkg = String.valueOf(mashupBundle.getSpecialOfferPackageId());

        Order order = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .agentId(agentProfileId)
                .network(normalizedNetwork)
                .phoneNumber(phoneNumber)
                .bundleCode(mashupBundle.getSlug())
                .bundleType("MASHUP")
                .packageId(resolvedPkg)
                .amount(total)
                .baseAmount(baseAmount)
                .costPrice(costPrice)
                .commissionAmount(commissionAmount)
                .status(Order.OrderStatus.CREATED.name())
                .providerStatus("processing")
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Mashup order created: id={}, packageId={}, base={}",
                order.getId(), resolvedPkg, baseAmount);

        if (userId != null) {
            Optional<Wallet> wallet = walletRepository.findByUserId(userId);
            if (wallet.isPresent() && wallet.get().getBalance().compareTo(total) >= 0) {
                return processWithWallet(order, wallet.get(), total, userId, email, normalizedNetwork);
            }
        }

        return initPaystack(order, email);
    }

    public List<Order> getByUserId(String userId, String orderId, String phoneNumber, String status) {
        if (orderId != null)
            return orderRepository.findById(orderId)
                    .filter(o -> o.getUserId().equals(userId))
                    .map(List::of).orElse(List.of());
        if (phoneNumber != null && status != null)
            return orderRepository.findByUserIdAndPhoneNumberAndStatus(userId, phoneNumber, status);
        if (phoneNumber != null)
            return orderRepository.findByUserIdAndPhoneNumber(userId, phoneNumber);
        if (status != null)
            return orderRepository.findByUserIdAndStatus(userId, status);
        return orderRepository.findByUserId(userId);
    }

    public Order getById(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId))
            throw new IllegalArgumentException("Order does not belong to user");
        return order;
    }

    public List<Order> getByAgentId(String agentUserId) {
        return orderRepository.findByAgentId(agentService.getProfileByUserId(agentUserId).getId());
    }

    /**
     * Get order by ID without authentication (used for payment verification callback).
     * Used when verifying payment status after Paystack redirects.
     */
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }


    public List<String> getDistinctCompletedPhoneNumbers() {
        return mongoTemplate.findDistinct(
                Query.query(Criteria.where("status").is(Order.OrderStatus.COMPLETED.name())),
                "phoneNumber",
                Order.class,
                String.class
        );
    }


    public List<SingleOrderUserDTO> getUsersWithSingleCompletedOrder() {

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(Order.OrderStatus.COMPLETED.name())),

                Aggregation.group("phoneNumber")
                        .count().as("orderCount")
                        .first("phoneNumber").as("phoneNumber")
                        .first("userId").as("userId"),

                Aggregation.match(Criteria.where("orderCount").is(1)),
                Aggregation.lookup("users", "userId", "_id", "userDetails"),
                Aggregation.unwind("userDetails"),

                Aggregation.project()
                        .and("phoneNumber").as("phoneNumber")
                        .and("userDetails.username").as("name")
                        .andExclude("_id")
        );

        AggregationResults<SingleOrderUserDTO> results = mongoTemplate.aggregate(
                aggregation, "orders", SingleOrderUserDTO.class
        );

        return results.getMappedResults();
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private boolean isMashupBundle(String bundleType) {
        return bundleType != null && "MASHUP".equalsIgnoreCase(bundleType.trim());
    }

    private Order initPaystack(Order order, String email) {
        try {
            String reference = "ORDER_" + order.getId();
            var response = paystackAdapter.initializeTransaction(
                    email, order.getAmount().multiply(BigDecimal.valueOf(100)).intValue(),
                    reference, callbackUrl);

            if (!response.isStatus() || response.getData() == null)
                throw new RuntimeException("Paystack init failed: " + response.getMessage());

            order.setPendingPayment(reference,
                    response.getData().getAuthorization_url(),
                    response.getData().getAccess_code());
            return orderRepository.save(order);
        } catch (Exception ex) {
            order.markFailed("Payment init failed: " + ex.getMessage());
            orderRepository.save(order);
            throw new RuntimeException(ex.getMessage());
        }
    }

    private Order processWithWallet(Order order, Wallet wallet, BigDecimal amount,
                                    String userId, String email, String network) {
        BigDecimal before = wallet.getBalance();
        var debitTx = transactionService.createDebit(userId, order.getId(), amount,
                before, before.subtract(amount), "Order payment for " + order.getBundleCode());

        if (wallet.getBalance().compareTo(amount) <= 0) {
            transactionService.fail(debitTx.getId());
            return initPaystack(order, email);
        }

        try {
            wallet.debit(amount);
            walletRepository.save(wallet);
            transactionService.complete(debitTx.getId());

            order.markPaid();
            order.markProcessing();
            order = orderRepository.save(order);

            String providerRef = buyBundle(order, network);
            order.markCompleted(providerRef);
            order = orderRepository.save(order);

            settleCommission(order);
            return order;

        } catch (Exception ex) {
            log.error("Wallet order failed: orderId={}", order.getId(), ex);
            transactionService.fail(debitTx.getId());
            order.markFailed(ex.getMessage());
            orderRepository.save(order);

            // Refund
            BigDecimal afterFail = wallet.getBalance();
            wallet.credit(amount);
            walletRepository.save(wallet);
            transactionService.createRefund(userId, order.getId(), amount,
                    afterFail, wallet.getBalance(), "Refund for failed order " + order.getId());
            order.markRefunded();
            orderRepository.save(order);

            return initPaystack(order, email);
        }
    }

    public void settleCommission(Order order) {
        if (order.getAgentId() != null
                && order.getCommissionAmount() != null
                && order.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Do not include Paystack fee (2%) in agent commission.
            // The stored commissionAmount = customerAmount - baseAmount (excludes fee).
            // Pass sellingAmount = baseAmount + commissionAmount so profit = commissionAmount.
            commissionService.settle(order.getAgentId(), order.getId(),
                    order.getBaseAmount(), order.getBaseAmount().add(order.getCommissionAmount()));
        }
    }

    private String buyBundle(Order order, String network) {
        boolean useRandyOnly = featureFlagService.isEnabled("bot.useRandyOnly", true);
        if (useRandyOnly) {
            // Route all orders through Randy when the feature flag is enabled
            order.setByFrom("randy");
            return order.getBundleType().equalsIgnoreCase("MASHUP")? automationService.buyFromRandyMashup(order) : automationService.buyFromRandy(order);
        }

        // Default behaviour: MTN uses Randy, others use the legacy bot
        if ("MTN".equalsIgnoreCase(network)) {
            order.setByFrom("randy");
            return order.getBundleType().equalsIgnoreCase("MASHUP")? automationService.buyFromRandyMashup(order) : automationService.buyFromRandy(order);
        }
        return automationService.buy(order);
    }

    private String resolvePackageId(String bundleCode, String network, String provided) {
//        if (provided != null) return provided;
        if (bundleCode == null) return null;
        String upper = bundleCode.toUpperCase();
        return Objects.equals(network, "MTN") ? PKG_MTN.get(upper) : PKG_TELECEL.get(upper);
    }
}
