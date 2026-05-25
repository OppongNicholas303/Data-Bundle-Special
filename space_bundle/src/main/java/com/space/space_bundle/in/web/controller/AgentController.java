package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.AgentBundlePricing;
import com.space.space_bundle.core.entities.AgentProfile;
import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.entities.Commission;
import com.space.space_bundle.core.services.AgentService;
import com.space.space_bundle.core.services.AgentStorefrontBundle;
import com.space.space_bundle.core.services.CommissionService;
import com.space.space_bundle.core.services.OrderService;
import com.space.space_bundle.core.services.WalletService;
import com.space.space_bundle.in.web.dto.*;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @org.springframework.beans.factory.annotation.Value("${app.storefront-base-url:https://tapdata.vercel.app/store}")
    private String storefrontBaseUrl;

    /**
     * GET /agents/register
     * Authenticated user registers as an agent reseller.
     */
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AgentProfile>> register(
            @Valid @RequestBody AgentRegisterRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        AgentProfile profile = agentService.registerAgent(userDetails.getUserId(), request.getBusinessName());
        return ResponseEntity.ok(ApiResponse.success("Agent registered successfully", profile));
    }

    /**
     * GET /agents/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentProfile>> getProfile(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        AgentProfile profile = agentService.getProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * GET /agents/my-link
     * Returns the agent's unique storefront referral link.
     */
    @GetMapping("/my-link")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMyLink(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        AgentProfile profile = agentService.getProfile(userDetails.getUserId());
        String link = storefrontBaseUrl + "/" + profile.getReferralCode();

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "referralCode", profile.getReferralCode(),
                "storefrontLink", link,
                "businessName", profile.getBusinessName()
        )));
    }

    /**
     * GET /agents/bundles
     * Returns all active platform bundles with agent's custom prices overlaid.
     */
    @GetMapping("/bundles")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<Bundle>>> getBundles(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        List<Bundle> bundles = agentService.getBundlesForAgent(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(bundles));
    }

    /**
     * GET /agents/bundles/pricing
     * Returns agent's custom pricing configurations.
     */
    @GetMapping("/bundles/pricing")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<AgentBundlePricing>>> getPricings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        List<AgentBundlePricing> pricings = agentService.getAgentPricings(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(pricings));
    }

    /**
     * PUT /agents/bundles/{bundleId}/price
     * Set or update agent's custom selling price for a bundle.
     * Enforces: sellingPrice >= basePrice
     */
    @PutMapping("/bundles/{bundleId}/price")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentBundlePricing>> setBundlePrice(
            @PathVariable String bundleId,
            @Valid @RequestBody SetBundlePriceRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        AgentBundlePricing pricing = agentService.setOrUpdateBundlePrice(
                userDetails.getUserId(), bundleId, request.getSellingPrice());
        return ResponseEntity.ok(ApiResponse.success("Bundle price updated", pricing));
    }

    /**
     * GET /agents/orders
     * Returns all orders placed through this agent.
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<?>> getOrders(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        var orders = orderService.getOrdersByAgentId(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * GET /agents/earnings
     * Returns agent's earnings summary: total sales, profit, wallet balance, commissions.
     */
    @GetMapping("/earnings")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentEarningsResponse>> getEarnings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();
        AgentProfile profile = agentService.getProfile(userId);
        List<Commission> commissions = commissionService.getAgentCommissions(profile.getId());

        AgentEarningsResponse response = AgentEarningsResponse.builder()
                .totalSales(profile.getTotalSales())
                .totalProfit(profile.getTotalProfit())
                .walletBalance(walletService.getBalance(userId))
                .commissions(commissions)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /agents/withdrawals
     * Initiate a Paystack transfer to agent's bank account.
     */
    @PostMapping("/withdrawals")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> withdraw(
            @Valid @RequestBody AgentWithdrawalRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        Map<String, Object> result = agentService.initiateWithdrawal(
                userDetails.getUserId(),
                request.getAmount(),
                request.getBankCode(),
                request.getAccountNumber(),
                request.getAccountName());

        return ResponseEntity.ok(ApiResponse.success("Withdrawal initiated", result));
    }

    // ─── Public Storefront Endpoints (no auth required) ───────────────────────

    /**
     * GET /agents/storefront/{agentCode}
     * Public endpoint — returns agent's business name + all bundles with their custom prices.
     * The frontend uses this to render the agent's branded order page.
     *
     * Example storefront URL the agent shares:
     *   https://tapdata.com/store/AGT-X7K2A
     *   (frontend calls this endpoint with the code from the URL)
     */
    @GetMapping("/storefront/{agentCode}")
    public ResponseEntity<ApiResponse<StorefrontResponse>> getStorefront(
            @PathVariable String agentCode) {

        AgentProfile profile = agentService.resolveByReferralCode(agentCode);
        List<AgentStorefrontBundle> bundles = agentService.getStorefrontBundles(agentCode);

        StorefrontResponse response = StorefrontResponse.builder()
                .agentCode(agentCode)
                .businessName(profile.getBusinessName())
                .bundles(bundles)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @lombok.Builder
    @lombok.Data
    public static class StorefrontResponse {
        private String agentCode;
        private String businessName;
        private List<AgentStorefrontBundle> bundles;
    }
}
