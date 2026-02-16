package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.OrderRepositoryPort;
import com.space.space_bundle.out.payment.PaystackAdapter;
import com.space.space_bundle.out.payment.dto.PaystackVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final OrderRepositoryPort orderRepository;
    private final PaystackAdapter paystackAdapter;
    private final AutomationPort automationPort;
    private final TransactionService transactionService;
    private final WalletService walletService;

    @Async
    @Transactional
    public void processPaystackWebhook(String payload) {
        log.info("[WEBHOOK] Starting to process payload");
        try {
            // Remove whitespace for easier parsing
            String cleanPayload = payload.replaceAll("\\s+", "");
            
            // Simple string parsing instead of Jackson
            if (cleanPayload.contains("\"event\":\"charge.success\"")) {
                log.info("[WEBHOOK] Event is charge.success");

                log.info("[WEBHOOK] Payload clean: {} " ,cleanPayload);
                log.info("[WEBHOOK] Payload : {} " ,payload);

                String reference = extractValue(payload, "reference");
                String status = extractValue(payload, "status");

                log.info("[WEBHOOK] Found reference: {}", reference);

                log.info("[WEBHOOK] Extracted - reference={}, status={}", reference, status);

                if ("success".equals(status)) {
                    log.info("[WEBHOOK] Status is success, processing payment");
                    processSuccessfulPayment(reference);
                } else {
                    log.warn("[WEBHOOK] Status is not success: {}", status);
                }
            } else {
                log.info("[WEBHOOK] Event is not charge.success, ignoring");
            }
            log.info("[WEBHOOK] Finished processing");
        } catch (Exception e) {
            log.error("[WEBHOOK] Failed to process: {}", e.getMessage(), e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        // Skip whitespace
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        
        // Check if value is a string (starts with ")
        if (json.charAt(startIndex) == '"') {
            startIndex++; // Skip opening quote
            int endIndex = json.indexOf("\"", startIndex);
            return json.substring(startIndex, endIndex).trim();
        } else {
            // Value is not a string (number, boolean, etc.)
            int endIndex = startIndex;
            while (endIndex < json.length() && 
                   json.charAt(endIndex) != ',' && 
                   json.charAt(endIndex) != '}' && 
                   json.charAt(endIndex) != ']') {
                endIndex++;
            }
            return json.substring(startIndex, endIndex).trim();
        }
    }

    private void processSuccessfulPayment(String reference) {
        log.info("[PAYMENT] Starting payment processing for reference: {}", reference);
        
        // Check if this is a wallet top-up
        if (reference.startsWith("TOPUP_")) {
            log.info("[PAYMENT] Processing wallet top-up: reference={}", reference);
            processTopUpPayment(reference);
            return;
        }
        
        // Verify transaction with Paystack
        log.info("[PAYMENT] Verifying transaction with Paystack");
        PaystackVerifyResponse verification = paystackAdapter.verifyTransaction(reference);

        log.info("[PAYMENT] Verification response: status={}, data={}",
                verification.isStatus(), verification.getData());

        if (!verification.isStatus() || !"success".equals(verification.getData().getStatus())) {
            log.error("[PAYMENT] Verification failed: reference={}", reference);
            return;
        }
        
        log.info("[PAYMENT] Verification successful");
        log.info("[PAYMENT] Transaction details - reference={}, amount={}, status={}",
                reference, verification.getData().getAmount(), verification.getData().getStatus());

        // Extract order ID from reference (ORDER_xxx)
        String orderId = reference.replace("ORDER_", "");
        log.info("[PAYMENT] Looking for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        log.info("[PAYMENT] Order found: orderId={}, status={}", orderId, order.getStatus());

        // Check if already processed
        if (order.getStatus() != com.space.space_bundle.core.enums.OrderStatus.PENDING_PAYMENT) {
            log.info("[PAYMENT] Order already processed: orderId={}, status={}", orderId, order.getStatus());
            return;
        }

        try {
            // Mark as paid
            log.info("[PAYMENT] Marking order as PAID");
            order.markPaid();
            order = orderRepository.save(order);

            // Create transaction record for Paystack payment
            if (order.getUserId() != null) {
                log.info("[PAYMENT] Creating transaction record");
                transactionService.createPaymentTransaction(
                    order.getUserId(), 
                    order.getId(), 
                    order.getAmount(), 
                    "Paystack payment for " + order.getBundleCode()
                );
            }

            // Send to bot for processing
            log.info("[PAYMENT] Marking order as PROCESSING");
            order.markProcessing();
            order = orderRepository.save(order);

            log.info("[PAYMENT] Calling bot API to deliver bundle");
//            String providerReference = automationPort.buyDataBundle(order);

            // Mark as completed
            log.info("[PAYMENT] Marking order as COMPLETED");
//            order.markCompleted(providerReference);
            order.markCompleted("11111111");
            order = orderRepository.save(order);

//            log.info("[PAYMENT] Order completed: orderId={}, providerRef={}", orderId, providerReference);

        } catch (Exception ex) {
            log.error("[PAYMENT] Order processing failed: orderId={}, error={}", orderId, ex.getMessage(), ex);
            order.markFailed("Bot processing failed: " + ex.getMessage());
            orderRepository.save(order);
        }
    }

    private void processTopUpPayment(String reference) {
        log.info("[TOPUP] Processing wallet top-up payment: reference={}", reference);
        
        PaystackVerifyResponse verification = paystackAdapter.verifyTransaction(reference);
        
        if (!verification.isStatus() || !"success".equals(verification.getData().getStatus())) {
            log.error("[TOPUP] Verification failed: reference={}", reference);
            return;
        }
        
        java.math.BigDecimal amount = java.math.BigDecimal.valueOf(verification.getData().getAmount())
            .divide(java.math.BigDecimal.valueOf(100));
        
        // Extract topUpId from reference
        String topUpId = reference.replace("TOPUP_", "");
        log.info("[TOPUP] Top-up verified: topUpId={}, amount={}", topUpId, amount);
        
        // Process top-up - the wallet service will handle finding the user
        try {
            walletService.processTopUpPaymentById(topUpId, amount);
            log.info("[TOPUP] Wallet topped up successfully: reference={}, amount={}", reference, amount);
        } catch (Exception ex) {
            log.error("[TOPUP] Failed to process top-up: reference={}, error={}", reference, ex.getMessage(), ex);
        }
    }
}
