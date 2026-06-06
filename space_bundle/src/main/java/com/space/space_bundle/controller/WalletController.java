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

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> balance(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                WalletBalanceResponse.builder()
                        .balance(walletService.getBalance(userDetails.getUserId()))
                        .currency("GHS")
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
}
