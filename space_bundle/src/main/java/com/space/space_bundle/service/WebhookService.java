package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.security.MoolreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final OrderRepository orderRepository;
    private final MoolreAdapter moolreAdapter;
    private final AutomationService automationService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final OrderService orderService;
    private final EmailService emailService;
    private final com.space.space_bundle.feature.FeatureFlagService featureFlagService;
    private final com.space.space_bundle.security.PaystackAdapter paystackAdapter;
    private final AsyncFulfillmentService asyncFulfillmentService;

    @Value("${app.support-email:support@tapdata.com}")
    private String supportEmail;

    @Value("${moolre.webhook-secret:default_secret}")
    private String moolreWebhookSecret;

    @Value("${paystack.secret-key:placeholder}")
    private String paystackSecretKey;

    // runtime flag read from DB via FeatureFlagService

    public boolean isValidMoolreWebhook(String payload) {
        String secret = extractValue(payload, "secret");
        if (secret == null || !secret.equals(moolreWebhookSecret)) {
            log.error("[WEBHOOK] Moolre secret mismatch. Expected={}, Got={}", moolreWebhookSecret, secret);
            return false;
        }
        return true;
    }

    @Async
    @Transactional
    public void processMoolre(String payload) {
        log.info("[WEBHOOK] Processing Moolre payload");
        try {
            String statusStr = extractValue(payload, "status");
            if (!"1".equals(statusStr)) return;

            String txstatusStr = extractValue(payload, "txstatus");
            if (!"1".equals(txstatusStr)) return;

            String reference = extractValue(payload, "externalref");
            if (reference == null) return;
            
            if (reference.startsWith("TOPUP_")) processTopUp(reference);
            else processOrderPayment(reference);
        } catch (Exception e) {
            log.error("[WEBHOOK] Failed: {}", e.getMessage(), e);
            emailService.send(supportEmail, "Webhook Error", e.getMessage());
            throw new RuntimeException("Webhook failed", e);
        }
    }

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
        log.info("[WEBHOOK] Processing Paystack payload");
        try {
            String clean = payload.replaceAll("\\s+", "");
            if (!clean.contains("\"event\":\"charge.success\"")) return;
            String reference = extractValue(payload, "reference");
            if (!"success".equals(extractValue(payload, "status"))) return;
            if (reference.startsWith("TOPUP_")) processPaystackTopUp(reference);
            else processPaystackOrderPayment(reference);
        } catch (Exception e) {
            log.error("[WEBHOOK] Failed: {}", e.getMessage(), e);
            emailService.send(supportEmail, "Webhook Error", e.getMessage());
            throw new RuntimeException("Webhook failed", e);
        }
    }

    private void processPaystackOrderPayment(String reference) {
        var verification = paystackAdapter.verifyTransaction(reference);
        if (!verification.isStatus() || !"success".equals(verification.getData().getStatus())) return;

        String orderId = reference.replace("ORDER_", "");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!Order.OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) return;

        fulfillVerifiedOrder(order);
    }

    private void processPaystackTopUp(String reference) {
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

    private void processOrderPayment(String reference) {
        var verification = moolreAdapter.checkPaymentStatus(reference, 1);
        if (!verification.containsKey("txstatus") || !Integer.valueOf(1).equals(verification.get("txstatus"))) return;

        BigDecimal amountPaid = BigDecimal.valueOf(Double.parseDouble(String.valueOf(verification.get("amount"))));

        String orderId = reference.replace("ORDER_", "");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!Order.OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) return;

        if (amountPaid.compareTo(order.getAmount()) < 0) {
            log.error("[WEBHOOK] Partial payment for order {}. Expected: {}, Paid: {}", orderId, order.getAmount(), amountPaid);
            return;
        }

        fulfillVerifiedOrder(order);
    }

    public boolean verifyOrderPayment(String orderId, String reference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!reference.equals(order.getPaymentReference())) {
            log.warn("[MANUAL_VERIFY] Reference {} does not match Order {} payment reference {}", reference, orderId, order.getPaymentReference());
            return false;
        }

        boolean verified = false;
        boolean isMoolre = order.getPaymentReference().equals(order.getPaymentAccessCode());
        BigDecimal amountPaid = BigDecimal.ZERO;

        if (isMoolre) {
            var verification = moolreAdapter.checkPaymentStatus(reference, 2);
            log.info("[MANUAL_VERIFY] Parsed Moolre verification: {}", verification);
            if (verification.containsKey("txstatus") && Integer.valueOf(1).equals(verification.get("txstatus"))) {
                verified = true;
                amountPaid = BigDecimal.valueOf(Double.parseDouble(String.valueOf(verification.get("amount"))));
            }
        } else {
            var verification = paystackAdapter.verifyTransaction(reference);
            log.info("[MANUAL_VERIFY] Parsed Paystack verification: {}", verification);
            if (verification != null && verification.isStatus() && verification.getData() != null && "success".equalsIgnoreCase(verification.getData().getStatus())) {
                verified = true;
                amountPaid = BigDecimal.valueOf(verification.getData().getAmount() / 100.0);
            }
        }

        if (!verified) {
            return false;
        }

        if (amountPaid.compareTo(order.getAmount()) < 0) {
            log.error("[MANUAL_VERIFY] Partial payment for order {}. Expected: {}, Paid: {}", orderId, order.getAmount(), amountPaid);
            return false;
        }

        if (!Order.OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            return true; // Already processed
        }

        return fulfillVerifiedOrder(order);
    }

    public boolean fulfillVerifiedOrder(Order order) {
        // Prevent race condition (webhook and manual verification simultaneously)
        boolean claimed = orderService.atomicClaimOrderForFulfillment(order.getId());
        if (!claimed) {
            log.info("Order {} was already claimed for fulfillment by another process", order.getId());
            return true;
        }
        order.setStatus(Order.OrderStatus.PAID.name()); // reflect in memory

        asyncFulfillmentService.executeFulfillment(order);
        return true;
    }

    public boolean verifyTopUpPayment(String topUpId, String reference) {
        boolean verified = false;
        BigDecimal amount = BigDecimal.ZERO;

        // Try Paystack if reference matches exactly our topup id
        String expectedReference = "TOPUP_" + topUpId;
        if (reference != null && reference.equals(expectedReference)) {
            try {
                var paystackVerification = paystackAdapter.verifyTransaction(reference);
                log.info("[MANUAL_VERIFY_TOPUP] Parsed Paystack verification: {}", paystackVerification);
                if (paystackVerification != null && paystackVerification.isStatus() &&
                        paystackVerification.getData() != null && "success".equalsIgnoreCase(paystackVerification.getData().getStatus())) {
                    verified = true;
                    amount = BigDecimal.valueOf(paystackVerification.getData().getAmount() / 100.0); // Convert from kobo to GHS
                }
            } catch (Exception e) {
                log.warn("[MANUAL_VERIFY_TOPUP] Paystack check failed for reference {}", reference);
            }
        } else if (reference != null && reference.startsWith("TOPUP_")) {
            log.warn("[MANUAL_VERIFY_TOPUP] Reference {} does not match expected reference {}", reference, expectedReference);
        }

        // Fallback to Moolre
        if (!verified) {
            try {
                var moolreVerification = moolreAdapter.checkPaymentStatus(reference, 2);
                log.info("[MANUAL_VERIFY_TOPUP] Parsed Moolre verification: {}", moolreVerification);
                if (moolreVerification.containsKey("txstatus") && Integer.valueOf(1).equals(moolreVerification.get("txstatus"))) {
                    String externalRef = (String) moolreVerification.get("externalref");
                    if (externalRef != null && externalRef.equals("TOPUP_" + topUpId)) {
                        verified = true;
                        amount = BigDecimal.valueOf(Double.parseDouble(String.valueOf(moolreVerification.get("amount"))));
                    } else {
                        log.warn("[MANUAL_VERIFY_TOPUP] Moolre ID {} belongs to externalref {} but topUpId is {}", reference, externalRef, topUpId);
                    }
                }
            } catch (Exception e) {
                log.warn("[MANUAL_VERIFY_TOPUP] Moolre check failed for reference {}", reference);
            }
        }

        if (!verified) {
            return false;
        }

        try {
            walletService.processTopUpById(topUpId, amount);
            return true;
        } catch (Exception ex) {
            log.error("[TOPUP] Manual verification failed: {}", topUpId, ex);
            return false;
        }
    }

    private void processTopUp(String reference) {
        var verification = moolreAdapter.checkPaymentStatus(reference, 1);
        if (!verification.containsKey("txstatus") || !Integer.valueOf(1).equals(verification.get("txstatus"))) return;
        BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(String.valueOf(verification.get("amount"))));
        try {
            walletService.processTopUpById(reference.replace("TOPUP_", ""), amount);
        } catch (Exception ex) {
            log.error("[TOPUP] Failed: {}", reference, ex);
            emailService.send(supportEmail, "TopUp Failed: " + reference, ex.getMessage());
        }
    }

    @Async
    @Transactional
    public void processLessData(java.util.Map<String, Object> payload) {
        try {
            log.info("[LESSDATA WEBHOOK] Processing webhook payload: {}", payload);
            String lessDataOrderId = (String) payload.get("id");
            String status = (String) payload.get("status");
            if (lessDataOrderId == null) {
                Object pObj = payload.get("payload");
                if (pObj instanceof java.util.Map) {
                    java.util.Map<?, ?> inner = (java.util.Map<?, ?>) pObj;
                    lessDataOrderId = (String) inner.get("id");
                    if (status == null) status = (String) inner.get("status");
                }
            }
            if (lessDataOrderId == null) {
                log.warn("[LESSDATA WEBHOOK] Missing order id in payload: {}", payload);
                return;
            }

            final String orderIdToSearch = lessDataOrderId;
            Order order = orderRepository.findByProviderOrderNumber(orderIdToSearch)
                    .orElseGet(() -> orderRepository.findByProviderReference(orderIdToSearch).orElse(null));

            if (order == null) {
                log.warn("[LESSDATA WEBHOOK] Order not found for LessData order ID: {}", lessDataOrderId);
                return;
            }

            log.info("[LESSDATA WEBHOOK] Found order id={}, current status={}, new status={}", order.getId(), order.getStatus(), status);
            if ("COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                if (!Order.OrderStatus.COMPLETED.name().equals(order.getStatus())) {
                    order.markCompleted(lessDataOrderId);
                    order.setProviderStatus("Delivered");
                    orderRepository.save(order);
                }
            } else if ("FAILED".equalsIgnoreCase(status)) {
                if (!Order.OrderStatus.FAILED.name().equals(order.getStatus())) {
                    order.markFailed("LessData webhook: Order failed");
                    order.setProviderStatus("Failed");
                    orderRepository.save(order);
                }
            }
        } catch (Exception e) {
            log.error("[LESSDATA WEBHOOK] Failed to process webhook: {}", e.getMessage(), e);
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
