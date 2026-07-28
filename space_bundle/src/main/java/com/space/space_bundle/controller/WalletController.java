package com.space.space_bundle.controller;

import com.space.space_bundle.dto.TopUpResponse;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.TopUpRequest;
import com.space.space_bundle.dto.WalletBalanceResponse;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final com.space.space_bundle.service.WebhookService webhookService;
    private final com.space.space_bundle.service.TransactionService transactionService;

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.List<com.space.space_bundle.entity.Transaction>>> getTransactions(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        java.util.List<com.space.space_bundle.entity.Transaction> txs = transactionService.getByUserId(userDetails.getUserId());
        // Sort by createdAt descending
        txs.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
        return ResponseEntity.ok(ApiResponse.success(txs));
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> balance(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        var wallet = walletService.getByUserId(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                WalletBalanceResponse.builder()
                        .balance(wallet.getBalance())
                        .commissionBalance(wallet.getCommissionBalance())
                        .currency(wallet.getCurrency())
                        .build()));
    }

    @PostMapping("/topup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TopUpResponse>> topUp(
            @RequestBody TopUpRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.initializeTopUp(userDetails.getUserId(), request.getAmount())));
    }

    @PostMapping("/topup/{id}/verify-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> verifyPayment(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        String reference = body.get("reference");
        if (reference == null || reference.trim().isEmpty()) {
            reference = body.get("moolreId"); // Fallback for backwards compatibility
        }
        if (reference == null || reference.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Payment reference is required"));
        }

        boolean verified = webhookService.verifyTopUpPayment(id, reference);

        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("Payment verified and top-up processed"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Payment could not be verified"));
        }
    }
}
