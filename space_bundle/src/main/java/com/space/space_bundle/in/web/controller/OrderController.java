package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.services.OrderService;
import com.space.space_bundle.in.web.dto.ApiResponse;
import com.space.space_bundle.in.web.dto.PlaceOrderRequest;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Order REST Controller
 * Handles data bundle order operations
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders - Place a new data bundle order
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Order placement request: userId={}, bundleCode={}, phoneNumber={}", 
                userId, request.getBundleCode(), request.getPhoneNumber());
        
        Order order = orderService.createOrder(
                userId,
                request.getNetwork(),
                request.getPhoneNumber(),
                request.getBundleCode()
        );
        
        log.info("Order placed successfully: orderId={}, status={}", order.getId(), order.getStatus());
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}