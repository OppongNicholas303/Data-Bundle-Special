package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.services.WalletService;
import com.space.space_bundle.in.web.dto.ApiResponse;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Wallet REST Controller
 * Handles wallet operations and balance management
 */
@Slf4j
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * GET /api/wallet/balance - Get current user's wallet balance
     */
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        BigDecimal balance = walletService.getBalance(userId);
        
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    /**
     * POST /api/wallet/credit - Credit wallet balance
     */
    @PostMapping("/credit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> credit(
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Wallet credit request: userId={}, amount={}", userId, amount);
        
        walletService.credit(userId, amount);
        
        log.info("Wallet credit successful: userId={}, amount={}", userId, amount);
        
        return ResponseEntity.ok(ApiResponse.success("Wallet credited successfully"));
    }

    /**
     * POST /api/wallet/debit - Debit wallet balance
     */
    @PostMapping("/debit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> debit(
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        
        log.info("Wallet debit request: userId={}, amount={}", userId, amount);
        
        walletService.debit(userId, amount);
        
        log.info("Wallet debit successful: userId={}, amount={}", userId, amount);
        
        return ResponseEntity.ok(ApiResponse.success("Wallet debited successfully"));
    }
}
