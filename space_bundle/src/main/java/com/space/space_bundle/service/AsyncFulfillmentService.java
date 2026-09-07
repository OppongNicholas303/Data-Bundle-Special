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
    private final BundleService bundleService;

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

            String providerRef = null;
            boolean useRandyOnly = featureFlagService.isEnabled("bot.useRandyOnly", false);
            boolean useLessData  = featureFlagService.isEnabled("bot.useLessData",  false);

            if ("MASHUP".equalsIgnoreCase(order.getBundleType())) {
                // User explicitly requested Mashup not to have dynamic routing
                if (useRandyOnly) {
                    order.setByFrom("randy");
                    providerRef = automationService.buyFromRandyMashup(order);
                } else {
                    order.setByFrom("mydatagigs");
                    providerRef = automationService.buy(order);
                }
            } else {
                com.space.space_bundle.entity.Bundle bundle = bundleService.getByCodeAndNetwork(order.getBundleCode(), order.getNetwork());
                String preferred = bundle.getPreferredProvider();
                if (preferred == null || preferred.isBlank() || preferred.equalsIgnoreCase("default")) {
                    // Fallback to global setting if no specific preference
                    preferred = useLessData ? "lessdata" : (useRandyOnly ? "randy" : "mydatagigs");
                }

                if ("lessdata".equalsIgnoreCase(preferred)) {
                    order.setByFrom("lessdata");
                    try {
                        providerRef = automationService.buyFromLessData(order);
                    } catch (Exception ex) {
                        log.warn("[FULFILLMENT] LessData failed for order {}, falling back to MyDataGigs. Error: {}", order.getId(), ex.getMessage());
                        order.setByFrom("mydatagigs");
                        providerRef = automationService.buy(order);
                    }
                } else if ("ramdy".equalsIgnoreCase(preferred) || "randy".equalsIgnoreCase(preferred)) {
                    order.setByFrom("randy");
                    try {
                        providerRef = automationService.buyFromRandy(order);
                    } catch (Exception ex) {
                        log.warn("[FULFILLMENT] Randy failed for order {}, falling back to MyDataGigs. Error: {}", order.getId(), ex.getMessage());
                        order.setByFrom("mydatagigs");
                        providerRef = automationService.buy(order);
                    }
                } else {
                    order.setByFrom("mydatagigs");
                    try {
                        providerRef = automationService.buy(order);
                    } catch (Exception ex) {
                        log.warn("[FULFILLMENT] MyDataGigs failed for order {}, falling back to Randy. Error: {}", order.getId(), ex.getMessage());
                        order.setByFrom("randy");
                        providerRef = automationService.buyFromRandy(order);
                    }
                }
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
