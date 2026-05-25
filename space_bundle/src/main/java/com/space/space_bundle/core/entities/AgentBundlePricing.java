package com.space.space_bundle.core.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AgentBundlePricing {
    private String id;
    private String agentId;
    private String bundleId;
    private BigDecimal basePrice;     // mirrors Bundle.sellingPrice — read-only reference
    private BigDecimal sellingPrice;  // agent's custom price, must be >= basePrice
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Agent profit per sale = sellingPrice - basePrice
     */
    public BigDecimal calculateProfit() {
        return sellingPrice.subtract(basePrice);
    }

    /**
     * Enforce: agent selling price must never be below platform base price
     */
    public void updateSellingPrice(BigDecimal newSellingPrice) {
        if (newSellingPrice.compareTo(basePrice) < 0) {
            throw new IllegalArgumentException(
                    "Selling price " + newSellingPrice + " cannot be lower than base price " + basePrice);
        }
        this.sellingPrice = newSellingPrice;
        this.updatedAt = LocalDateTime.now();
    }
}
