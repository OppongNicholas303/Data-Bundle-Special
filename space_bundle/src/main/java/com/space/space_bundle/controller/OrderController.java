package com.space.space_bundle.controller;

import com.space.space_bundle.dto.SingleOrderUserDTO;
import com.space.space_bundle.entity.Order;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.PlaceOrderRequest;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.space.space_bundle.service.WebhookService;
import java.util.Map;
import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        System.out.println(request.getNetwork());
        String email = userDetails != null ? userDetails.getEmail()
                : "nictech" + (10000 + new Random().nextInt(90000)) + "@gmail.com";

        if((request.getAgentCode() != null) && (request.getEmail() != null)){
            email = request.getEmail();
        }

        Order order = orderService.placeOrder(
                request.getNetwork(), request.getPhoneNumber(), request.getBundleCode(),
                email, userDetails != null ? userDetails.getUserId() : null,
                request.getPackage_id(), request.getAgentCode(), request.getBundleType(),
                request.getRedirectUrl());

        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Order>>> getOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getByUserId(userDetails.getUserId(), orderId, phoneNumber, status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> getById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getById(id, userDetails.getUserId())));
    }

    @GetMapping("/completed/phone-numbers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getDistinctCompletedPhoneNumbers() {
        List<String> phoneNumbers = orderService.getDistinctCompletedPhoneNumbers();
        return ResponseEntity.ok(phoneNumbers);
    }

    @GetMapping("/completed/single-order-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SingleOrderUserDTO>> getUsersWithSingleCompletedOrder() {
        return ResponseEntity.ok(orderService.getUsersWithSingleCompletedOrder());
    }

    @PostMapping("/{id}/verify-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> verifyPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        String moolreId = body.get("moolreId");
        if (moolreId == null || moolreId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Moolre ID is required"));
        }

        boolean verified = webhookService.verifyOrderWithMoolreId(id, moolreId);

        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("Payment verified and order is processing"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Payment could not be verified"));
        }
    }
}
