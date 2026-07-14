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

    @Value("${app.support-email:support@tapdata.com}")
    private String supportEmail;

    @Value("${moolre.webhook-secret:default_secret}")
    private String moolreWebhookSecret;

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

    private void processOrderPayment(String reference) {
        var verification = moolreAdapter.checkPaymentStatus(reference);
        if (!verification.containsKey("txstatus") || !Integer.valueOf(1).equals(verification.get("txstatus"))) return;

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
                providerRef = order.getBundleType().equalsIgnoreCase("MASHUP")? automationService.buyFromRandyMashup(order) : automationService.buyFromRandy(order);
            } else {
                // Preserve prior behavior: MTN => Randy, others => legacy bot
                if ("MTN".equalsIgnoreCase(order.getNetwork())) {
                    order.setByFrom("randy");
                    providerRef = order.getBundleType().equalsIgnoreCase("MASHUP")? automationService.buyFromRandyMashup(order) : automationService.buyFromRandy(order);
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
        var verification = moolreAdapter.checkPaymentStatus(reference);
        if (!verification.containsKey("txstatus") || !Integer.valueOf(1).equals(verification.get("txstatus"))) return;
        BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(String.valueOf(verification.get("amount"))));
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
