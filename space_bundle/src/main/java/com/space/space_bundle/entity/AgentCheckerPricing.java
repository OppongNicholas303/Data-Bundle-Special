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
@Document(collection = "agent_checker_pricing")
@CompoundIndex(name = "agent_checker_unique", def = "{'agentId': 1, 'serviceName': 1}", unique = true)
public class AgentCheckerPricing {

    @Id
    private String id;
    private String agentId;
    private String serviceName;

    @JsonIgnore  // Never expose platform base price to customers via agent APIs
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal calculateProfit() {
        if (sellingPrice == null || basePrice == null) return BigDecimal.ZERO;
        return sellingPrice.subtract(basePrice);
    }
}
