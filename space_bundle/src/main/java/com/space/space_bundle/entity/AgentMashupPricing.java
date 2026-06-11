package com.space.space_bundle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_mashup_pricing")
@CompoundIndex(name = "agent_mashup_unique", def = "{'agentId': 1, 'mashupBundleId': 1}", unique = true)
public class AgentMashupPricing {

    @Id
    private String id;

    private String agentId;

    /** The MashupBundle document ID */
    private String mashupBundleId;

    /** Admin's base selling price at the time of setting — never exposed to customers */
    @JsonIgnore
    private BigDecimal basePrice;

    /** Agent's custom selling price (must be >= basePrice) */
    private BigDecimal sellingPrice;

    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal calculateProfit() {
        if (sellingPrice == null || basePrice == null) return BigDecimal.ZERO;
        return sellingPrice.subtract(basePrice);
    }
}
