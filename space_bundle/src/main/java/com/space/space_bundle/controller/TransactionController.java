package com.space.space_bundle.controller;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.entity.Transaction;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.OrderService;
import com.space.space_bundle.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Transaction>>> getAll(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByUserId(userDetails.getUserId())));
    }

    /**
     * Verify payment status by order ID.
     * Called by frontend after Paystack redirects back.
     * Handles both "ORDER_id" format and plain "id" format.
     * Returns order amount, reference, and status (success/pending/failed).
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> verifyPaymentByOrder(@PathVariable String orderId) {
        // Handle both "ORDER_xxx" and plain "xxx" formats
        String cleanId = orderId.replace("ORDER_", "");
        
        try {
            Order order = orderService.getOrderById(cleanId);
            
            // Map order status to payment status
            String paymentStatus = "failed"; // default
            if ("PAID".equals(order.getStatus()) || "PROCESSING".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
                paymentStatus = "success";
            } else if ("PENDING_PAYMENT".equals(order.getStatus())) {
                paymentStatus = "pending";
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", paymentStatus);
            response.put("amount", order.getAmount().doubleValue());
            response.put("reference", "ORDER_" + order.getId());
            response.put("channel", "paystack");
            response.put("orderId", order.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Order not found or other error
            throw new IllegalArgumentException("Order not found: " + cleanId);
        }
    }
}
