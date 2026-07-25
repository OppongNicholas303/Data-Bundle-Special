package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.BotPurchaseResponseRandy;
import com.space.space_bundle.dto.MyDataGigsStatusResponse;
import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.OrderRepository;
import com.space.space_bundle.service.AutomationService;
import com.space.space_bundle.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    private final OrderRepository orderRepository;
    private final AutomationService automationService;
    private final OrderService orderService;

    @GetMapping("/track-order/{phoneNumber}")
    public ResponseEntity<ApiResponse<Order>> trackOrder(@PathVariable String phoneNumber) {
        return orderRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .map(order -> {
                    if ("PENDING".equals(order.getStatus())) {
                        try {
                            if ("randy".equalsIgnoreCase(order.getByFrom()) && order.getProviderOrderNumber() != null) {
                                BotPurchaseResponseRandy statusResp = automationService.checkStatus(order.getProviderOrderNumber());
                                if (statusResp != null && statusResp.order() != null) {
                                    String extStatus = statusResp.order().status();
                                    updateLocalOrderStatus(order, extStatus, order.getProviderOrderNumber());
                                }
                            } else if (!"randy".equalsIgnoreCase(order.getByFrom()) && order.getProviderReference() != null) {
                                MyDataGigsStatusResponse statusResp = automationService.checkMyDataGigsStatus(order.getProviderReference());
                                if (statusResp != null) {
                                    String extStatus = statusResp.getOrder_status();
                                    updateLocalOrderStatus(order, extStatus, order.getProviderReference());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to fetch external status for order {}: {}", order.getId(), e.getMessage());
                        }
                    }
                    return ResponseEntity.ok(ApiResponse.success(order));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("No recent order found for this number")));
    }

    private void updateLocalOrderStatus(Order order, String extStatus, String referenceId) {
        if ("Delivered".equalsIgnoreCase(extStatus) || "COMPLETED".equalsIgnoreCase(extStatus) || "SUCCESS".equalsIgnoreCase(extStatus)) {
            order.markCompleted(referenceId);
            orderService.settleCommission(order);
            orderRepository.save(order);
        } else if ("FAILED".equalsIgnoreCase(extStatus) || "Cancelled".equalsIgnoreCase(extStatus)) {
            order.markFailed("Failed at external vendor");
            orderRepository.save(order);
            // Refund logic is normally handled asynchronously by webhook, but we just mark it failed here.
        }
    }
}
