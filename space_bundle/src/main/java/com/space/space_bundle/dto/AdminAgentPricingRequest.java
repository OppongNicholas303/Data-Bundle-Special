package com.space.space_bundle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAgentPricingRequest {

    @NotBlank(message = "Agent profile ID is required")
    private String agentProfileId;

    @NotBlank(message = "Bundle ID is required")
    private String bundleId;

    /**
     * The base price this specific agent pays the platform per sale.
     * Must be >= bundle.costPrice (platform acquisition cost).
     * Agent's selling price must always be >= this value.
     */
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", message = "Base price cannot be negative")
    private BigDecimal basePrice;

    /**
     * Optionally pre-set the agent's selling price too.
     * If null, agent keeps their current selling price or defaults to basePrice.
     */
    @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;
}
