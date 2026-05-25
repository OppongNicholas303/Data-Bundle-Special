package com.space.space_bundle.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_bundle_pricing")
@CompoundIndex(name = "agent_bundle_unique", def = "{'agentId': 1, 'bundleId': 1}", unique = true)
public class AgentBundlePricingDocument {
    @Id
    private String id;
    private String agentId;
    private String bundleId;
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
