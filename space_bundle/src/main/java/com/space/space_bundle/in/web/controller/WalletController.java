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

@Slf4j
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Wallet>> getBalance(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        
        String userId = userDetails.getUserId();
        log.info("Get wallet balance request: userId={}", userId);
        
        Wallet wallet = walletService.getWalletByUserId(userId);
        
        log.info("Wallet balance retrieved: userId={}, balance={}, currency={}, status={}", 
                userId, wallet.getBalance(), wallet.getCurrency(), wallet.getStatus());
        
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }
}
