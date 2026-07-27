package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFulfillmentService {

    private final OrderRepository orderRepository;
    private final AutomationService automationService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final OrderService orderService;
    private final EmailService emailService;
    private final com.space.space_bundle.feature.FeatureFlagService featureFlagService;

    @Value("${app.support-email:support@tapdata.com}")
    private String supportEmail;

    @Async
    public void executeFulfillment(Order order) {
        try {
            if (order.getUserId() != null) {
                BigDecimal bal = walletService.getBalance(order.getUserId());
                transactionService.createPayment(order.getUserId(), order.getId(),
                        order.getAmount(), bal, bal, "Paystack payment for " + order.getBundleCode());
            }

            order.markProcessing();
            orderRepository.save(order);

            String providerRef;
            boolean useRandyOnly = featureFlagService.isEnabled("bot.useRandyOnly", false);
            if (useRandyOnly) {
                order.setByFrom("randy");
                providerRef = "MASHUP".equalsIgnoreCase(order.getBundleType()) ? automationService.buyFromRandyMashup(order) : automationService.buyFromRandy(order);
            } else {
                order.setByFrom("mydatagigs");
                providerRef = automationService.buy(order);
            }

            order.markCompleted(providerRef);
            orderRepository.save(order);

            orderService.settleCommission(order);
            log.info("[FULFILLMENT] Order completed: {}", order.getId());

        } catch (com.space.space_bundle.exception.ProviderTimeoutException ex) {
            log.error("[FULFILLMENT] Order provider timeout: {}", order.getId(), ex);
            emailService.send(supportEmail, "Order Indeterminate Timeout: " + order.getId(), ex.getMessage());
            order.markProcessingUnknown();
            orderRepository.save(order);
        } catch (Exception ex) {
            log.error("[FULFILLMENT] Order failed: {}", order.getId(), ex);
            emailService.send(supportEmail, "Order Failed: " + order.getId(), ex.getMessage());
            order.markFailed("Processing failed: " + ex.getMessage());
            orderRepository.save(order);
        }
    }
}
