package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.security.MoolreAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final OrderRepository orderRepository;
    private final WebhookService webhookService;
    private final MoolreAdapter moolreAdapter;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.support-email:support@tapdata.com}")
    private String supportEmail;

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void reconcilePendingOrders() {
        log.info("[RECONCILIATION] Starting reconciliation job for pending orders...");
        LocalDateTime from = LocalDateTime.now().minusHours(24);
        LocalDateTime to = LocalDateTime.now().minusMinutes(5);

        List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtBetween(Order.OrderStatus.PENDING_PAYMENT.name(), from, to);
        if (pendingOrders.isEmpty()) {
            log.info("[RECONCILIATION] No stale pending orders found.");
            return;
        }

        log.info("[RECONCILIATION] Found {} pending orders. Verifying status with Moolre...", pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                if (order.getPaymentReference() == null) continue;
                
                var verification = moolreAdapter.checkPaymentStatus(order.getPaymentReference(), 1);
                
                if (verification.containsKey("txstatus")) {
                    Integer txstatus = null;
                    Object statusObj = verification.get("txstatus");
                    if (statusObj instanceof Integer) {
                        txstatus = (Integer) statusObj;
                    } else if (statusObj instanceof String) {
                        txstatus = Integer.parseInt((String) statusObj);
                    }

                    if (Integer.valueOf(1).equals(txstatus)) {
                        log.info("[RECONCILIATION] Order {} payment successful. Fulfilling...", order.getId());
                        webhookService.fulfillVerifiedOrder(order);
                    } else if (Integer.valueOf(2).equals(txstatus)) {
                        log.info("[RECONCILIATION] Order {} payment failed. Marking as failed.", order.getId());
                        order.markFailed("Payment failed according to Moolre reconciliation");
                        orderRepository.save(order);
                    }
                }
            } catch (Exception e) {
                log.error("[RECONCILIATION] Error verifying order {}: {}", order.getId(), e.getMessage());
            }
        }

        // Handle PROCESSING_UNKNOWN
        LocalDateTime fromUnknown = LocalDateTime.now().minusMinutes(15);
        LocalDateTime toUnknown = LocalDateTime.now().minusMinutes(5);
        List<Order> unknownOrders = orderRepository.findByStatusAndCreatedAtBetween(Order.OrderStatus.PROCESSING_UNKNOWN.name(), fromUnknown, toUnknown);
        
        for (Order order : unknownOrders) {
            log.warn("[RECONCILIATION] Order {} is PROCESSING_UNKNOWN. Needs manual check.", order.getId());
            try {
                emailService.send(supportEmail, "Manual Intervention Required: Order " + order.getId(),
                        "Order " + order.getId() + " for " + order.getPhoneNumber() + 
                        " is in PROCESSING_UNKNOWN state due to a provider timeout. " +
                        "Please check the provider dashboard (MyDataGigs/Randy) manually to verify if the bundle was sent, " +
                        "and then manually complete or fail/refund the order.");
            } catch (Exception e) {
                log.error("Failed to send email for unknown order {}", order.getId(), e);
            }
        }

        log.info("[RECONCILIATION] Reconciliation job completed.");
    }
}
