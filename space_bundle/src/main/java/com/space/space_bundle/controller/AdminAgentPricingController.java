package com.space.space_bundle.controller;

import com.space.space_bundle.dto.AdminAgentPricingRequest;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.entity.AgentBundlePricing;
import com.space.space_bundle.entity.AgentCheckerPricing;
import com.space.space_bundle.entity.AgentMashupPricing;
import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.repository.AgentProfileRepository;
import com.space.space_bundle.service.AgentService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/agents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgentPricingController {

    private final AgentService agentService;
    private final AgentProfileRepository agentProfileRepository;

    /**
     * GET /admin/agents/{agentProfileId}/pricing
     * View all bundle prices configured for a specific agent.
     */
    @GetMapping({"/{agentProfileId}/pricing", "/{agentProfileId}/bundles/pricing"})
    public ResponseEntity<ApiResponse<List<AgentBundlePricing>>> getAgentPricing(
            @PathVariable String agentProfileId) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.adminGetPricingsForAgent(agentProfileId)));
    }

    /**
     * GET /admin/agents/pricing/bundle/{bundleId}
     * View all agents' prices for a specific bundle — side-by-side comparison.
     */
    @GetMapping("/pricing/bundle/{bundleId}")
    public ResponseEntity<ApiResponse<List<AgentBundlePricing>>> getPricingForBundle(
            @PathVariable String bundleId) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.adminGetPricingsForBundle(bundleId)));
    }

    /**
     * PUT /admin/agents/pricing
     * Set base price for a specific agent on a specific bundle.
     *
     * Example body:
     * {
     *   "agentProfileId": "abc-123",
     *   "bundleId": "def-456",
     *   "basePrice": 8.00,       <- what platform earns per sale from this agent
     *   "sellingPrice": 10.00    <- optional: pre-set the agent's customer-facing price
     * }
     *
     * Agent 1 → MTN 2GB basePrice=8.00  → can sell at 8.00+
     * Agent 2 → MTN 2GB basePrice=9.50  → can sell at 9.50+
     */
    @PutMapping("/pricing")
    public ResponseEntity<ApiResponse<AgentBundlePricing>> setAgentBasePrice(
            @Valid @RequestBody AdminAgentPricingRequest request) {
        AgentBundlePricing result = agentService.adminSetAgentBasePrice(
                request.getAgentProfileId(),
                request.getBundleId(),
                request.getBasePrice(),
                request.getSellingPrice());
        return ResponseEntity.ok(ApiResponse.success("Base price updated for agent", result));
    }

    /**
     * PUT /admin/agents/{agentProfileId}/bundles/{bundleId}/price
     * Set the selling price for one agent on one bundle.
     */
    @PutMapping("/{agentProfileId}/bundles/{bundleId}/price")
    public ResponseEntity<ApiResponse<AgentBundlePricing>> setAgentSellingPrice(
            @PathVariable String agentProfileId,
            @PathVariable String bundleId,
            @RequestBody AdminAgentPricingRequest request) {
        BigDecimal sellingPrice = request.getSellingPrice();
        if (sellingPrice == null)
            throw new IllegalArgumentException("sellingPrice is required");

        AgentBundlePricing result = agentService.adminSetAgentBasePrice(
                agentProfileId,
                bundleId,
                request.getBasePrice() != null ? request.getBasePrice() : sellingPrice,
                sellingPrice);
        return ResponseEntity.ok(ApiResponse.success("Agent selling price updated", result));
    }

    /**
     * GET /admin/agents/{agentProfileId}/mashup/pricing
     * View all Mashup prices configured for a specific agent.
     */
    @GetMapping("/{agentProfileId}/mashup/pricing")
    public ResponseEntity<ApiResponse<List<AgentMashupPricing>>> getAgentMashupPricing(
            @PathVariable String agentProfileId) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.adminGetMashupPricingsForAgent(agentProfileId)));
    }

    /**
     * GET /admin/agents/mashup/pricing/bundle/{mashupBundleId}
     * View all agents' prices for a specific Mashup bundle.
     */
    @GetMapping("/mashup/pricing/bundle/{mashupBundleId}")
    public ResponseEntity<ApiResponse<List<AgentMashupPricing>>> getMashupPricingForBundle(
            @PathVariable String mashupBundleId) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.adminGetMashupPricingsForBundle(mashupBundleId)));
    }

    /**
     * PUT /admin/agents/mashup/pricing
     * Set base/selling price for a specific agent on a specific Mashup bundle.
     */
    @PutMapping("/mashup/pricing")
    public ResponseEntity<ApiResponse<AgentMashupPricing>> setAgentMashupBasePrice(
            @Valid @RequestBody AdminAgentPricingRequest request) {
        AgentMashupPricing result = agentService.adminSetAgentMashupPrice(
                request.getAgentProfileId(),
                request.getBundleId(),
                request.getBasePrice(),
                request.getSellingPrice());
        return ResponseEntity.ok(ApiResponse.success("Mashup base price updated for agent", result));
    }

    /**
     * PUT /admin/agents/{agentProfileId}/mashup/{mashupBundleId}/price
     * Set the selling price for one agent on one Mashup bundle.
     */
    @PutMapping("/{agentProfileId}/mashup/{mashupBundleId}/price")
    public ResponseEntity<ApiResponse<AgentMashupPricing>> setAgentMashupSellingPrice(
            @PathVariable String agentProfileId,
            @PathVariable String mashupBundleId,
            @RequestBody AdminAgentPricingRequest request) {
        BigDecimal sellingPrice = request.getSellingPrice();
        if (sellingPrice == null)
            throw new IllegalArgumentException("sellingPrice is required");

        AgentMashupPricing result = agentService.adminSetAgentMashupPrice(
                agentProfileId,
                mashupBundleId,
                request.getBasePrice() != null ? request.getBasePrice() : sellingPrice,
                sellingPrice);
        return ResponseEntity.ok(ApiResponse.success("Agent Mashup selling price updated", result));
    }

    /**
     * PUT /admin/agents/mashup/pricing/bulk
     * Apply the same Mashup base price to ALL agents for a bundle at once.
     */
    @PutMapping("/mashup/pricing/bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setBulkMashupBasePrice(
            @RequestBody BulkPricingRequest request) {
        int updated = agentService.adminSetMashupPriceForAllAgents(
                request.getBundleId(), request.getBasePrice());
        return ResponseEntity.ok(ApiResponse.success("Bulk update successful",
                Map.of("agentsUpdated", updated)));
    }

    // ── Checker Pricing Endpoints ──────────────────────────────────────────

    @GetMapping("/{agentProfileId}/checker/pricing")
    public ResponseEntity<ApiResponse<List<AgentCheckerPricing>>> getAgentCheckerPricing(
            @PathVariable String agentProfileId) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.adminGetCheckerPricingsForAgent(agentProfileId)));
    }

    @GetMapping("/checker/pricing/service/{serviceName}")
    public ResponseEntity<ApiResponse<List<AgentCheckerPricing>>> getCheckerPricingForService(
            @PathVariable String serviceName) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.adminGetCheckerPricingsForService(serviceName)));
    }

    @PutMapping("/checker/pricing")
    public ResponseEntity<ApiResponse<AgentCheckerPricing>> setAgentCheckerBasePrice(
            @Valid @RequestBody AdminAgentPricingRequest request) {
        AgentCheckerPricing result = agentService.adminSetAgentCheckerPrice(
                request.getAgentProfileId(),
                request.getBundleId(), // we use bundleId field to carry serviceName
                request.getBasePrice(),
                request.getSellingPrice());
        return ResponseEntity.ok(ApiResponse.success("Checker base price updated for agent", result));
    }

    @PutMapping("/{agentProfileId}/checker/{serviceName}/price")
    public ResponseEntity<ApiResponse<AgentCheckerPricing>> setAgentCheckerSellingPrice(
            @PathVariable String agentProfileId,
            @PathVariable String serviceName,
            @RequestBody AdminAgentPricingRequest request) {
        BigDecimal sellingPrice = request.getSellingPrice();
        if (sellingPrice == null)
            throw new IllegalArgumentException("sellingPrice is required");

        AgentCheckerPricing result = agentService.adminSetAgentCheckerPrice(
                agentProfileId,
                serviceName,
                request.getBasePrice() != null ? request.getBasePrice() : sellingPrice,
                sellingPrice);
        return ResponseEntity.ok(ApiResponse.success("Agent Checker selling price updated", result));
    }

    @PutMapping("/checker/pricing/bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setBulkCheckerBasePrice(
            @RequestBody BulkPricingRequest request) {
        int updated = agentService.adminSetCheckerBasePriceForAllAgents(
                request.getBundleId(), // we use bundleId field to carry serviceName
                request.getBasePrice());
        return ResponseEntity.ok(ApiResponse.success("Bulk update successful",
                Map.of("agentsUpdated", updated)));
    }

    /**
     * PUT /admin/agents/pricing/bulk
     * Apply the same base price to ALL agents for a bundle at once.
     */
    @PutMapping("/pricing/bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setBulkBasePrice(
            @RequestBody BulkPricingRequest request) {
        if (request.getBundleId() == null || request.getBasePrice() == null)
            throw new IllegalArgumentException("bundleId and basePrice are required");

        int updated = agentService.adminSetBasePriceForAllAgents(
                request.getBundleId(), request.getBasePrice());

        return ResponseEntity.ok(ApiResponse.success("Bulk update complete",
                Map.of("bundleId", request.getBundleId(),
                       "basePrice", request.getBasePrice(),
                       "agentsUpdated", updated)));
    }

    @Data
    public static class BulkPricingRequest {
        private String bundleId;
        private BigDecimal basePrice;
    }
}
