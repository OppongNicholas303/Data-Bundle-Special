package com.space.space_bundle.core.services;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * What a customer sees when they open an agent's storefront link.
 * Contains the agent's custom selling price — base price is intentionally hidden from customers.
 */
@Data
@Builder
public class AgentStorefrontBundle {
    private String bundleId;
    private String bundleCode;
    private String name;
    private String dataSize;
    private String network;
    private BigDecimal sellingPrice;  // agent's price (or platform price if no custom price set)
    private String agentCode;         // referral code — frontend embeds this in the order request
}
