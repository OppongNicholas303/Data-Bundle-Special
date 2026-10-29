package com.space.space_bundle.service;

import com.space.space_bundle.dto.SingleOrderUserDTO;
import com.space.space_bundle.dto.ExternalOrderStatusDto;
import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.entity.MashupBundle;
import com.space.space_bundle.entity.Order;
import com.space.space_bundle.entity.Transaction;
import com.space.space_bundle.entity.Wallet;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.repository.WalletRepository;
import com.space.space_bundle.security.MoolreAdapter;
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
    private final MoolreAdapter moolreAdapter;
    private final com.space.space_bundle.security.PaystackAdapter paystackAdapter;
    private final AutomationService automationService;
    private final com.space.space_bundle.feature.FeatureFlagService featureFlagService;
    private final MongoTemplate mongoTemplate;
    private final WalletService walletService;

    @Value("${moolre.callback-url:http://localhost:8080/api/webhook/moolre}")
    private String moolreCallbackUrl;

    @Value("${moolre.redirect-url:http://localhost:3000/payment/callback}")
    private String moolreRedirectUrl;

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
        return placeOrder(network, phoneNumber, bundleCode, email, userId, packageId, null, null, null, null);
    }

    @Transactional
    public Order placeOrder(String network, String phoneNumber, String bundleCode,
                            String email, String userId, String packageId, String agentCode) {
        return placeOrder(network, phoneNumber, bundleCode, email, userId, packageId, agentCode, null, null, null);
    }

    @Transactional
    public Order placeOrder(String network, String phoneNumber, String bundleCode,
                            String email, String userId, String packageId, String agentCode,
                            String bundleType, String redirectUrl, String paymentMethod) {
        return placeOrder(network, phoneNumber, bundleCode, email, userId, packageId, agentCode,
                bundleType, redirectUrl, paymentMethod, false);
    }

    public Order placeOrder(String network, String phoneNumber, String bundleCode,
                            String email, String userId, String packageId, String agentCode,
                            String bundleType, String redirectUrl, String paymentMethod, boolean buyerIsAgent) {

        if (isMashupBundle(bundleType)) {
            return placeMashupOrder(network, phoneNumber, bundleCode, email, userId, packageId, agentCode, redirectUrl, paymentMethod, buyerIsAgent);
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
            String bundleId = bundleService.getByCodeAndNetwork(bundleCode, normalizedNetwork).getId();
            // baseAmount = what platform earns = agent's assigned base price
            baseAmount = agentService.resolveAgentBasePrice(agentUserId, bundleId, baseAmount);
            customerAmount = agentService.resolveEffectivePrice(agentUserId, bundleId);
            commissionAmount = customerAmount.subtract(baseAmount);
        } else if (userId != null && (buyerIsAgent || agentService.isActiveAgentUser(userId))) {
            String bundleId = bundleService.getByCodeAndNetwork(bundleCode, normalizedNetwork).getId();
            baseAmount = agentService.resolveAgentBasePrice(userId, bundleId, baseAmount);
            customerAmount = baseAmount;
        }

        BigDecimal total = customerAmount;

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
            Optional<Wallet> wallet = walletRepository.findFirstByUserId(userId);
            if (wallet.isPresent() && wallet.get().getBalance().compareTo(total) >= 0) {
                return processWithWallet(order, wallet.get(), total, userId, email, normalizedNetwork, redirectUrl, paymentMethod);
            }
        }

        return initializePayment(order, email, redirectUrl, paymentMethod);
    }

    private Order placeMashupOrder(String network, String phoneNumber, String bundleCode,
                                   String email, String userId, String packageId, String agentCode, String redirectUrl, String paymentMethod, boolean buyerIsAgent) {
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
        } else if (userId != null && (buyerIsAgent || agentService.isActiveAgentUser(userId))) {
            baseAmount = agentService.resolveAgentMashupBasePrice(userId, mashupBundle.getId(), baseAmount);
            customerAmount = baseAmount;
        }

        BigDecimal total = customerAmount;
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
            Optional<Wallet> wallet = walletRepository.findFirstByUserId(userId);
            if (wallet.isPresent() && wallet.get().getBalance().compareTo(total) >= 0) {
                return processWithWallet(order, wallet.get(), total, userId, email, normalizedNetwork, redirectUrl, paymentMethod);
            }
        }

        return initializePayment(order, email, redirectUrl, paymentMethod);
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
     * Used when verifying payment status after Moolre redirects.
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

    public boolean atomicClaimOrderForFulfillment(String orderId) {
        Query query = new Query(Criteria.where("id").is(orderId).and("status").is(Order.OrderStatus.PENDING_PAYMENT.name()));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .set("status", Order.OrderStatus.PAID.name())
                .set("updatedAt", LocalDateTime.now());
        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, Order.class);
        return result.getModifiedCount() > 0;
    }

    public boolean atomicClaimFailedOrderForReprocessing(String orderId) {
        Query query = new Query(Criteria.where("id").is(orderId).and("status").in(Order.OrderStatus.FAILED.name(), Order.OrderStatus.PROCESSING.name()));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .set("status", Order.OrderStatus.PROCESSING.name())
                .set("updatedAt", LocalDateTime.now());
        com.mongodb.client.result.UpdateResult result = mongoTemplate.updateFirst(query, update, Order.class);
        return result.getModifiedCount() > 0;
    }

    public Order reprocessFailedOrder(String orderId) {
        return reprocessFailedOrder(orderId, "unknown-admin");
    }

    public Order reprocessFailedOrder(String orderId, String adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        String originalStatus = order.getStatus();
        if (!List.of(Order.OrderStatus.FAILED.name(), Order.OrderStatus.PROCESSING.name(),
                Order.OrderStatus.PROCESSING_UNKNOWN.name()).contains(originalStatus)) {
            throw new IllegalStateException("Replacement is only allowed for FAILED or stale PROCESSING orders. Current status: " + originalStatus);
        }

        PaymentCheck payment;
        try {
            payment = verifyReplacementPayment(order);
        } catch (Exception ex) {
            recordBlockedReplacement(order, adminId, "PAYMENT_CHECK_ERROR", "NOT_CHECKED",
                    "Payment provider could not verify the original charge; no replacement was sent.");
            throw new IllegalStateException("Could not verify original payment. Replacement is blocked; retry after the payment provider is available.", ex);
        }
        if (!payment.paid()) {
            recordBlockedReplacement(order, adminId, payment.source(), "NOT_CHECKED", payment.message());
            throw new IllegalStateException(payment.message());
        }

        if (Order.OrderStatus.PROCESSING.name().equals(originalStatus)
                || Order.OrderStatus.PROCESSING_UNKNOWN.name().equals(originalStatus)) {
            LocalDateTime lastChanged = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
            if (lastChanged == null || lastChanged.isAfter(LocalDateTime.now().minusMinutes(10))) {
                String message = "Order is still within the 10-minute processing window. Wait for provider reconciliation before replacing it.";
                recordBlockedReplacement(order, adminId, payment.source(), "NOT_CHECKED", message);
                throw new IllegalStateException(message);
            }
            if (order.getProviderReference() == null || order.getProviderReference().isBlank()) {
                String message = "Provider has not returned an order reference. Delivery cannot be checked safely; this order needs manual provider review.";
                recordBlockedReplacement(order, adminId, payment.source(), "NOT_CHECKED", message);
                throw new IllegalStateException(message);
            }
        }

        if (!claimReplacement(order, adminId)) {
            String message = "Order is currently being processed or another admin already claimed its replacement.";
            recordBlockedReplacement(order, adminId, payment.source(), "NOT_CHECKED", message);
            throw new IllegalStateException(message);
        }
        order = orderRepository.findById(orderId).orElseThrow();
        Order.ReplacementAttempt attempt = Order.ReplacementAttempt.builder()
                .id(UUID.randomUUID().toString()).adminId(adminId).attemptedAt(LocalDateTime.now())
                .paymentCheck(payment.source()).providerCheck("NOT_CHECKED").result("STARTED").build();
        order.recordReplacementAttempt(attempt);
        orderRepository.save(order);

        if (!Order.OrderStatus.FAILED.name().equals(originalStatus)
                || (order.getProviderReference() != null && !order.getProviderReference().isBlank())) {
            ExternalOrderStatusDto provider = automationService.checkExternalStatus(order);
            String outcome = classifyProviderStatus(provider.getStatus());
            attempt.setProviderCheck(provider.getProvider() + ":" + provider.getStatus());
            if ("DELIVERED".equals(outcome)) {
                String verifiedProviderRef = provider.getProviderOrderId() != null
                        ? provider.getProviderOrderId() : order.getProviderReference();
                order.markCompleted(verifiedProviderRef);
                attempt.setResult("ALREADY_DELIVERED");
                attempt.setDetail("Provider confirmed delivery; no replacement was sent.");
                orderRepository.save(order);
                settleCommission(order);
                return order;
            }
            if (!"FAILED".equals(outcome)) {
                order.setStatus(originalStatus);
                attempt.setResult("BLOCKED");
                attempt.setDetail("Provider did not confirm terminal failure; no replacement was sent.");
                orderRepository.save(order);
                throw new IllegalStateException("Provider status is " + provider.getStatus() + ". Replacement is blocked until delivery or failure is confirmed.");
            }
        } else {
            attempt.setProviderCheck(order.getProviderReference() == null ? "NO_PROVIDER_ORDER" : "FAILED_ORDER");
        }

        try {
            String providerRef = buyBundle(order, order.getNetwork());
            order.markCompleted(providerRef);
            attempt.setResult("COMPLETED");
            attempt.setDetail("Replacement delivery confirmed by provider response.");
            orderRepository.save(order);

            settleCommission(order);
            log.info("[REPROCESS] Order completed: {}", order.getId());
            return order;

        } catch (com.space.space_bundle.exception.ProviderTimeoutException ex) {
            log.error("[REPROCESS] Order provider timeout: {}", order.getId(), ex);
            order.markProcessingUnknown();
            attempt.setResult("PROVIDER_OUTCOME_UNKNOWN");
            attempt.setDetail("Provider timed out. Reconcile provider status before another replacement.");
            orderRepository.save(order);
            throw new RuntimeException("Reprocessing provider timeout: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("[REPROCESS] Order failed: {}", order.getId(), ex);
            if (isDefinitiveProviderRejection(ex)) {
                order.markFailed("Reprocessing failed: " + ex.getMessage());
                attempt.setResult("FAILED");
                attempt.setDetail(ex.getMessage());
            } else {
                order.markProcessingUnknown();
                attempt.setResult("PROVIDER_OUTCOME_UNKNOWN");
                attempt.setDetail("Provider response was ambiguous. Reconcile provider status before another replacement.");
            }
            orderRepository.save(order);
            throw new RuntimeException("Reprocessing failed: " + ex.getMessage());
        }
    }

    private void recordBlockedReplacement(Order order, String adminId, String paymentCheck,
                                          String providerCheck, String detail) {
        LocalDateTime now = LocalDateTime.now();
        Order.ReplacementAttempt attempt = Order.ReplacementAttempt.builder()
                .id(UUID.randomUUID().toString()).adminId(adminId).attemptedAt(now)
                .paymentCheck(paymentCheck).providerCheck(providerCheck).result("BLOCKED").detail(detail).build();
        Query query = new Query(Criteria.where("id").is(order.getId()).and("status").is(order.getStatus()));
        var update = new org.springframework.data.mongodb.core.query.Update()
                .push("replacementHistory", attempt)
                .inc("replacementAttempts", 1)
                .set("lastReplacementBy", adminId)
                .set("lastReplacementAt", now);
        mongoTemplate.updateFirst(query, update, Order.class);
    }

    private boolean isDefinitiveProviderRejection(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof org.springframework.web.reactive.function.client.WebClientResponseException response
                    && response.getStatusCode().is4xxClientError()) return true;
            current = current.getCause();
        }
        return false;
    }

    private boolean claimReplacement(Order order, String adminId) {
        Query query = new Query(Criteria.where("id").is(order.getId()).and("status").is(order.getStatus()));
        if (Order.OrderStatus.PROCESSING.name().equals(order.getStatus())
                || Order.OrderStatus.PROCESSING_UNKNOWN.name().equals(order.getStatus())) {
            LocalDateTime lastChanged = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
            if (lastChanged == null) return false;
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
            if (order.getUpdatedAt() != null) {
                query.addCriteria(Criteria.where("updatedAt").lte(cutoff));
            } else {
                query.addCriteria(new Criteria().andOperator(
                        Criteria.where("createdAt").lte(cutoff),
                        new Criteria().orOperator(Criteria.where("updatedAt").exists(false), Criteria.where("updatedAt").is(null))));
            }
        }
        var update = new org.springframework.data.mongodb.core.query.Update()
                .set("status", Order.OrderStatus.PROCESSING.name())
                .set("lastReplacementBy", adminId)
                .set("lastReplacementAt", LocalDateTime.now())
                .inc("replacementAttempts", 1)
                .set("updatedAt", LocalDateTime.now());
        return mongoTemplate.updateFirst(query, update, Order.class).getModifiedCount() > 0;
    }

    private PaymentCheck verifyReplacementPayment(Order order) {
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            return new PaymentCheck(false, "INVALID_ORDER_AMOUNT", "Order amount is missing or invalid; replacement is blocked.");
        List<Transaction> transactions = transactionService.getTransactionsByOrderId(order.getId());
        BigDecimal refunds = transactions.stream()
                .filter(tx -> Transaction.Type.REFUND.name().equalsIgnoreCase(tx.getType()))
                .filter(tx -> !Transaction.Status.FAILED.name().equalsIgnoreCase(tx.getStatus())
                        && !Transaction.Status.CANCELLED.name().equalsIgnoreCase(tx.getStatus()))
                .map(tx -> tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getPaymentReference() != null && !order.getPaymentReference().isBlank()) {
            if (order.getPaymentReference().equals(order.getPaymentAccessCode())) {
                var verification = moolreAdapter.checkPaymentStatus(order.getPaymentReference(), 2);
                Integer status = parseInteger(verification.get("txstatus"));
                if (!Integer.valueOf(1).equals(status))
                    return new PaymentCheck(false, "MOOLRE_" + status, "Moolre has not confirmed successful payment. Current status: " + status);
                if (refunds.compareTo(BigDecimal.ZERO) > 0)
                    return new PaymentCheck(false, "REFUNDED", "This order has already been refunded; replacement is blocked.");
                return new PaymentCheck(true, "MOOLRE_VERIFIED", "Payment verified");
            }
            var verification = paystackAdapter.verifyTransaction(order.getPaymentReference());
            if (verification == null || !verification.isStatus() || verification.getData() == null
                    || !"success".equalsIgnoreCase(verification.getData().getStatus()))
                return new PaymentCheck(false, "PAYSTACK_NOT_PAID", "Paystack has not confirmed successful payment.");
            var data = verification.getData();
            int expectedPesewas = order.getAmount().multiply(new BigDecimal("1.02"))
                    .multiply(new BigDecimal("100")).intValue();
            if (!"GHS".equalsIgnoreCase(data.getCurrency()) || data.getAmount() == null || data.getAmount() < expectedPesewas)
                return new PaymentCheck(false, "PAYSTACK_AMOUNT_MISMATCH", "Verified Paystack amount or currency does not cover this order.");
            if (refunds.compareTo(BigDecimal.ZERO) > 0)
                return new PaymentCheck(false, "REFUNDED", "This order has already been refunded; replacement is blocked.");
            return new PaymentCheck(true, "PAYSTACK_VERIFIED", "Payment verified");
        }

        if (order.getUserId() == null || order.getUserId().isBlank())
            return new PaymentCheck(false, "NO_PAYMENT_RECORD", "No payment reference or wallet owner is recorded; replacement is blocked.");
        BigDecimal completedDebits = transactions.stream()
                .filter(tx -> Transaction.Type.DEBIT.name().equalsIgnoreCase(tx.getType()))
                .filter(tx -> Transaction.Status.COMPLETED.name().equalsIgnoreCase(tx.getStatus()))
                .map(tx -> tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (completedDebits.subtract(refunds).compareTo(order.getAmount()) < 0)
            return new PaymentCheck(false, "WALLET_NOT_PAID", "Completed wallet debit records do not prove the full order amount remains paid.");
        return new PaymentCheck(true, "WALLET_VERIFIED", "Wallet debit verified");
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String string) {
            try { return Integer.valueOf(string); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private String classifyProviderStatus(String status) {
        if (status == null) return "UNKNOWN";
        String value = status.trim().toLowerCase(Locale.ROOT);
        if (value.contains("fail") || value.contains("reject") || value.contains("cancel")) return "FAILED";
        if (List.of("completed", "complete", "delivered", "success", "successful").contains(value)
                || value.startsWith("completed ") || value.startsWith("delivered ")) return "DELIVERED";
        if (value.contains("pending") || value.contains("processing") || value.contains("queued")
                || value.contains("in progress") || value.contains("in_progress")) return "PENDING";
        return "UNKNOWN";
    }

    private record PaymentCheck(boolean paid, String source, String message) {}


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

    public ExternalOrderStatusDto checkExternalStatus(Order order) {
        return automationService.checkExternalStatus(order);
    }

    // ── Queries ────────────────────────────────────────────────────────────

    private boolean isMashupBundle(String bundleType) {
        return bundleType != null && "MASHUP".equalsIgnoreCase(bundleType.trim());
    }

    private Order initMoolre(Order order, String email, String redirectUrl) {
        try {
            String reference = "ORDER_" + order.getId();
            String finalRedirectUrl = (redirectUrl != null && !redirectUrl.isBlank()) ? redirectUrl : moolreRedirectUrl;
            var responseData = moolreAdapter.generatePaymentLink(
                    order.getAmount().doubleValue(), email,
                    reference, moolreCallbackUrl, finalRedirectUrl);

            String authUrl = (String) responseData.get("authorization_url");
            String moolreRef = (String) responseData.get("reference");
            if (moolreRef == null) moolreRef = reference;

            order.setPendingPayment(moolreRef, authUrl, moolreRef);
            return orderRepository.save(order);
        } catch (Exception ex) {
            order.markFailed("Payment init failed: " + ex.getMessage());
            orderRepository.save(order);
            throw new RuntimeException(ex.getMessage());
        }
    }

    private Order processWithWallet(Order order, Wallet wallet, BigDecimal amount,
                                    String userId, String email, String network, String redirectUrl, String paymentMethod) {
        BigDecimal before = wallet.getBalance();

        if (wallet.getBalance().compareTo(amount) < 0) {
            return initializePayment(order, email, redirectUrl, paymentMethod);
        }

        var debitTx = transactionService.createDebit(userId, order.getId(), amount,
                before, before.subtract(amount), "Order payment for " + order.getBundleCode());

        try {
            java.math.BigDecimal newBalance = walletService.atomicDebit(userId, amount);
            if (newBalance == null) {
                throw new IllegalStateException("Insufficient wallet balance or concurrent update");
            }
            transactionService.complete(debitTx.getId());

            order.markPaid();
            order.markProcessing();
            order = orderRepository.save(order);

            String providerRef = buyBundle(order, network);
            order.markCompleted(providerRef);
            order = orderRepository.save(order);

            settleCommission(order);
            return order;

        } catch (com.space.space_bundle.exception.ProviderTimeoutException ex) {
            log.error("Wallet order provider timeout: orderId={}", order.getId(), ex);
            order.markProcessingUnknown();
            order = orderRepository.save(order);
            return order;
        } catch (Exception ex) {
            log.error("Wallet order failed: orderId={}", order.getId(), ex);
            transactionService.fail(debitTx.getId());
            order.markFailed(ex.getMessage());
            orderRepository.save(order);

            // Refund
            BigDecimal afterFail = walletService.getBalance(userId); // Get fresh balance just in case
            BigDecimal trueAfter = walletService.atomicCredit(userId, amount);
            transactionService.createRefund(userId, order.getId(), amount,
                    afterFail, trueAfter != null ? trueAfter : afterFail.add(amount), "Refund for failed order " + order.getId());
            order.markRefunded();
            orderRepository.save(order);

            return order;
        }
    }

    private Order initializePayment(Order order, String email, String redirectUrl, String paymentMethod) {
        boolean paystackEnabled = featureFlagService.isEnabled("payment.paystack.enabled", false);
        boolean moolreEnabled = featureFlagService.isEnabled("payment.moolre.enabled", true);

        if (!paystackEnabled && !moolreEnabled) {
            throw new IllegalStateException("External checkout is disabled. Only wallet payments are allowed.");
        }

        if ("PAYSTACK".equalsIgnoreCase(paymentMethod) && paystackEnabled) {
            return initPaystack(order, email, redirectUrl);
        } else if ("MOOLRE".equalsIgnoreCase(paymentMethod) && moolreEnabled) {
            return initMoolre(order, email, redirectUrl);
        }

        // Fallback to whichever is enabled if paymentMethod is null or invalid
        if (moolreEnabled) {
            return initMoolre(order, email, redirectUrl);
        } else {
            return initPaystack(order, email, redirectUrl);
        }
    }

    private Order initPaystack(Order order, String email, String callbackUrl) {
        try {
            String reference = "ORDER_" + order.getId();
            if (callbackUrl == null) callbackUrl = moolreRedirectUrl;

            // Apply 2% surcharge for Paystack to cover fees
            BigDecimal chargeMultiplier = BigDecimal.valueOf(1.02);
            int paystackAmountInPesewas = order.getAmount().multiply(chargeMultiplier)
                    .multiply(BigDecimal.valueOf(100)).intValue();

            var response = paystackAdapter.initializeTransaction(
                    email, paystackAmountInPesewas,
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

    public void settleCommission(Order order) {
        if (order.getAgentId() != null
                && order.getCommissionAmount() != null
                && order.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Do not include Moolre fee in agent commission.
            // The stored commissionAmount = customerAmount - baseAmount (excludes fee).
            // Pass sellingAmount = baseAmount + commissionAmount so profit = commissionAmount.
            commissionService.settle(order.getAgentId(), order.getId(),
                    order.getBaseAmount(), order.getBaseAmount().add(order.getCommissionAmount()));
        }
    }

    private String buyBundle(Order order, String network) {
        boolean useRandyOnly   = featureFlagService.isEnabled("bot.useRandyOnly",    false);
        boolean useLessData    = featureFlagService.isEnabled("bot.useLessData",     true);

        String preferred = "mydatagigs";
        try {
            com.space.space_bundle.entity.Bundle bundle = bundleService.getByCodeAndNetwork(order.getBundleCode(), order.getNetwork());
            if (bundle != null && bundle.getPreferredProvider() != null) {
                preferred = bundle.getPreferredProvider();
            }
        } catch (Exception ignored) {}

        if (preferred == null || preferred.isBlank() || preferred.equalsIgnoreCase("default") || preferred.equalsIgnoreCase("mydatagigs")) {
            preferred = useLessData ? "lessdata" : (useRandyOnly ? "randy" : "mydatagigs");
        }

        if ("lessdata".equalsIgnoreCase(preferred)) {
            order.setByFrom("lessdata");
            log.info("[ORDER] Routing to LessData: orderId={}", order.getId());
            return automationService.buyFromLessData(order);
        } else if ("randy".equalsIgnoreCase(preferred) || "ramdy".equalsIgnoreCase(preferred)) {
            order.setByFrom("randy");
            return order.getBundleType() != null && order.getBundleType().equalsIgnoreCase("MASHUP")
                    ? automationService.buyFromRandyMashup(order)
                    : automationService.buyFromRandy(order);
        } else {
            order.setByFrom("mydatagigs");
            return automationService.buy(order);
        }
    }

    private String resolvePackageId(String bundleCode, String network, String provided) {
//        if (provided != null) return provided;
        if (bundleCode == null) return null;
        String upper = bundleCode.toUpperCase();
        return Objects.equals(network, "MTN") ? PKG_MTN.get(upper) : PKG_TELECEL.get(upper);
    }
}
