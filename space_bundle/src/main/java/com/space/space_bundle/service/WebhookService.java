package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.security.PaystackAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final OrderRepository orderRepository;
    private final PaystackAdapter paystackAdapter;
    private final AutomationService automationService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final OrderService orderService;
    private final EmailService emailService;
    private final com.space.space_bundle.feature.FeatureFlagService featureFlagService;

    @Value("${app.support-email:support@tapdata.com}")
    private String supportEmail;

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    // runtime flag read from DB via FeatureFlagService

    /**
     * Verifies the x-paystack-signature header.
     * Paystack signs the raw request body with HMAC-SHA512 using your secret key.
     * We must compute the same hash and compare — reject if they don't match.
     */
    public boolean isValidSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[WEBHOOK] Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Async
    @Transactional
    public void processPaystack(String payload) {
        log.info("[WEBHOOK] Processing");
        try {
            String clean = payload.replaceAll("\\s+", "");
            if (!clean.contains("\"event\":\"charge.success\"")) return;
            String reference = extractValue(payload, "reference");
            if (!"success".equals(extractValue(payload, "status"))) return;
            if (reference.startsWith("TOPUP_")) processTopUp(reference);
            else processOrderPayment(reference);
        } catch (Exception e) {
            log.error("[WEBHOOK] Failed: {}", e.getMessage(), e);
            emailService.send(supportEmail, "Webhook Error", e.getMessage());
            throw new RuntimeException("Webhook failed", e);
        }
    }

    private void processOrderPayment(String reference) {
        var verification = paystackAdapter.verifyTransaction(reference);
        if (!verification.isStatus() || !"success".equals(verification.getData().getStatus())) return;

        String orderId = reference.replace("ORDER_", "");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!Order.OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) return;

        try {
            order.markPaid();
            orderRepository.save(order);

            if (order.getUserId() != null) {
                BigDecimal bal = walletService.getBalance(order.getUserId());
                transactionService.createPayment(order.getUserId(), order.getId(),
                        order.getAmount(), bal, bal, "Paystack payment for " + order.getBundleCode());
            }

            order.markProcessing();
            orderRepository.save(order);

            String providerRef;
            boolean useRandyOnly = featureFlagService.isEnabled("bot.useRandyOnly", true);
            if (useRandyOnly) {
                order.setByFrom("randy");
                providerRef = automationService.buyFromRandy(order);
            } else {
                // Preserve prior behavior: MTN => Randy, others => legacy bot
                if ("MTN".equalsIgnoreCase(order.getNetwork())) {
                    order.setByFrom("randy");
                    providerRef = automationService.buyFromRandy(order);
                } else {
                    providerRef = automationService.buy(order);
                }
            }

            order.markCompleted(providerRef);
            orderRepository.save(order);

            orderService.settleCommission(order);
            log.info("[WEBHOOK] Order completed: {}", orderId);

        } catch (Exception ex) {
            log.error("[WEBHOOK] Order failed: {}", orderId, ex);
            emailService.send(supportEmail, "Order Failed: " + orderId, ex.getMessage());
            order.markFailed("Processing failed: " + ex.getMessage());
            orderRepository.save(order);
        }
    }

    private void processTopUp(String reference) {
        var verification = paystackAdapter.verifyTransaction(reference);
        if (!verification.isStatus() || !"success".equals(verification.getData().getStatus())) return;
        BigDecimal amount = BigDecimal.valueOf(verification.getData().getAmount())
                .divide(BigDecimal.valueOf(100));
        try {
            walletService.processTopUpById(reference.replace("TOPUP_", ""), amount);
        } catch (Exception ex) {
            log.error("[TOPUP] Failed: {}", reference, ex);
            emailService.send(supportEmail, "TopUp Failed: " + reference, ex.getMessage());
        }
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (json.charAt(start) == '"') {
            start++;
            return json.substring(start, json.indexOf("\"", start)).trim();
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }
}
