package com.space.space_bundle.controller;

import com.space.space_bundle.dto.AgentStorefrontBundle;
import com.space.space_bundle.entity.AgentBundlePricing;
import com.space.space_bundle.entity.AgentMashupPricing;
import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.entity.Commission;
import com.space.space_bundle.dto.*;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.AgentService;
import com.space.space_bundle.service.CommissionService;
import com.space.space_bundle.service.OrderService;
import com.space.space_bundle.service.WalletService;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final CommissionService commissionService;
    private final OrderService orderService;
    private final WalletService walletService;

    @Value("${app.storefront-base-url:https://tapdata.vercel.app/store}")
    private String storefrontBaseUrl;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AgentProfile>> register(
            @Valid @RequestBody AgentRegisterRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Agent registered. Pending admin approval.",
                agentService.register(userDetails.getUserId(), request.getBusinessName())));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentProfile>> profile(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.getProfileByUserId(userDetails.getUserId())));
    }

    @GetMapping("/my-link")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> myLink(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        AgentProfile profile = agentService.getProfileByUserId(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "referralCode", profile.getReferralCode(),
                "storefrontLink", storefrontBaseUrl + "/" + profile.getReferralCode(),
                "businessName", profile.getBusinessName())));
    }

    @GetMapping("/bundles/pricing")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<AgentBundlePricing>>> pricings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.getPricings(userDetails.getUserId())));
    }

    @PutMapping("/bundles/{bundleId}/price")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentBundlePricing>> setPrice(
            @PathVariable String bundleId,
            @Valid @RequestBody SetBundlePriceRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Price updated",
                agentService.setPrice(userDetails.getUserId(), bundleId, request.getSellingPrice())));
    }

    @GetMapping("/mashup/pricing")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<AgentMashupPricing>>> mashupPricings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.getMashupPricings(userDetails.getUserId())));
    }

    @PutMapping("/mashup/{mashupBundleId}/price")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentMashupPricing>> setMashupPrice(
            @PathVariable String mashupBundleId,
            @Valid @RequestBody SetBundlePriceRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Mashup price updated",
                agentService.setMashupPrice(userDetails.getUserId(), mashupBundleId, request.getSellingPrice())));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<?>>> orders(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getByAgentId(userDetails.getUserId())));
    }

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentEarningsResponse>> earnings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        AgentProfile profile = agentService.getProfileByUserId(userId);
        List<Commission> commissions = commissionService.getByAgentId(profile.getId());
        return ResponseEntity.ok(ApiResponse.success(AgentEarningsResponse.builder()
                .totalSales(profile.getTotalSales())
                .totalProfit(profile.getTotalProfit())
                .walletBalance(walletService.getBalance(userId))
                .commissions(commissions)
                .build()));
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<com.space.space_bundle.entity.WithdrawalRequest>> withdraw(
            @Valid @RequestBody AgentWithdrawalRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request submitted. Pending admin approval.",
                agentService.initiateWithdrawal(userDetails.getUserId(), request.getAmount(),
                        request.getMomoProvider(), request.getMomoNumber(), request.getAccountName())));
    }

    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<com.space.space_bundle.entity.WithdrawalRequest>>> getWithdrawals(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.getWithdrawals(userDetails.getUserId())));
    }

    // ── Public storefront ──────────────────────────────────────────────────

    @GetMapping("/storefront/{agentCode}")
    public ResponseEntity<ApiResponse<StorefrontResponse>> storefront(@PathVariable String agentCode) {
        log.info("[STOREFRONT] Request agentCode='{}'", agentCode);
        AgentProfile profile = agentService.resolveByCode(agentCode);
        List<AgentStorefrontBundle> bundles = agentService.getStorefrontByProfile(profile, agentCode);
        return ResponseEntity.ok(ApiResponse.success(StorefrontResponse.builder()
                .agentCode(agentCode)
                .businessName(profile.getBusinessName())
                .bundles(bundles)
                .build()));
    }

    @Data @Builder
    public static class StorefrontResponse {
        private String agentCode;
        private String businessName;
        private List<AgentStorefrontBundle> bundles;
    }
}
