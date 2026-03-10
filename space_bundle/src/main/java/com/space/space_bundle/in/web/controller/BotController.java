package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.OrderRepositoryPort;
import com.space.space_bundle.out.automation.MyDataGigsBotService;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponseRandy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    private final MyDataGigsBotService botService;
    private final OrderRepositoryPort orderRepository;
    private final AutomationPort automationPort;

    @GetMapping("/track-order/{phoneNumber}")
    public ResponseEntity<MyDataGigsBotService.OrderStatus> trackOrder(@PathVariable String phoneNumber) {
        Optional<Order> latestOrder = orderRepository.findLatestByPhoneNumber(phoneNumber);

        if (latestOrder.isPresent()) {
            Order order = latestOrder.get();
            log.info("last order {}, ", order);
            if ("randy".equalsIgnoreCase(order.getByFrom())) {
                log.info("getting from randy");
                try {
                    BotPurchaseResponseRandy randyStatus = automationPort
                            .checkOrderStatusFromRandy(order.getProviderOrderNumber());
                    if (randyStatus != null && randyStatus.order() != null) {
                        return ResponseEntity.ok(new MyDataGigsBotService.OrderStatus(
                                randyStatus.order().order_number(),
                                randyStatus.order().status(),
                                randyStatus.order().customer_phone(),
                                randyStatus.order().package_name(),
                                randyStatus.order().cost_price(),
                                randyStatus.order().created_at().toString()));
                    }
                } catch (Exception e) {
                    // Fall back to botService if Randy check fails or use regular log
                }
            }
        }

        // Fallback to existing botService tracking or if no order found
        MyDataGigsBotService.OrderStatus status = botService.trackOrder(phoneNumber);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
