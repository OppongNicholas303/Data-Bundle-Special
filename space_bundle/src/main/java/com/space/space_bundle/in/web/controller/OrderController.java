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
import java.util.List;
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
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails
    ) {

        String email = "nictech23@gmail.com";

        if(userDetails  != null){
            email = userDetails.getEmail();
        }else {
            email = request.getEmail() != null && !request.getEmail().isEmpty()
                    ? request.getEmail()
                    : email;
        }

        
        log.info("Order placement request: bundleCode={}, phoneNumber={}, email={}", 
                request.getBundleCode(), request.getPhoneNumber(), request.getEmail());

        // Use provided email or generate from phone number

        Order order = orderService.createGuestOrder(
                request.getNetwork(),
                request.getPhoneNumber(),
                request.getBundleCode(),
                email,
                userDetails != null ? userDetails.getUserId() : null
        );

        log.info("Creating order :{}", order);
        
        log.info("Order placed successfully: orderId={}, status={}", order.getId(), order.getStatus());
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * GET /api/orders - Get all orders with optional filters
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Get orders request: userId={}, orderId={}, phoneNumber={}, status={}", 
                userId, orderId, phoneNumber, status);
        
        List<Order> orders = orderService.getOrders(userId, orderId, phoneNumber, status);
        
        log.info("Orders retrieved: count={}", orders.size());
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * GET /api/orders/{id} - Get order by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Get order by ID request: userId={}, orderId={}", userId, id);
        
        Order order = orderService.getOrderById(id, userId);
        
        log.info("Order retrieved: orderId={}, status={}", order.getId(), order.getStatus());
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}