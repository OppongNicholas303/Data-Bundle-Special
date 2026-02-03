package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.core.services.TransactionService;
import com.space.space_bundle.in.web.dto.ApiResponse;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transaction REST Controller
 * Handles transaction history and operations
 */
@Slf4j
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * GET /api/transactions - Get current user's transaction history
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserTransactions(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Transaction history request: userId={}", userId);
        
        List<Transaction> transactions = transactionService.getUserTransactions(userId);
        
        log.info("Transaction history retrieved: userId={}, count={}", userId, transactions.size());
        
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * GET /api/transactions/order/{orderId} - Get transactions for specific order
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Transaction>>> getOrderTransactions(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Order transactions request: userId={}, orderId={}", userId, orderId);
        
        List<Transaction> transactions = transactionService.getOrderTransactions(orderId);
        
        log.info("Order transactions retrieved: orderId={}, count={}", orderId, transactions.size());
        
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}