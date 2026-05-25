package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.AgentBundlePricing;
import com.space.space_bundle.out.persistence.entity.AgentBundlePricingDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AgentBundlePricingMapper {

    public static AgentBundlePricingDocument toDocument(AgentBundlePricing pricing) {
        return AgentBundlePricingDocument.builder()
                .id(pricing.getId())
                .agentId(pricing.getAgentId())
                .bundleId(pricing.getBundleId())
                .basePrice(pricing.getBasePrice())
                .sellingPrice(pricing.getSellingPrice())
                .active(pricing.isActive())
                .createdAt(pricing.getCreatedAt())
                .updatedAt(pricing.getUpdatedAt())
                .build();
    }

    public static AgentBundlePricing toDomain(AgentBundlePricingDocument doc) {
        return AgentBundlePricing.builder()
                .id(doc.getId())
                .agentId(doc.getAgentId())
                .bundleId(doc.getBundleId())
                .basePrice(doc.getBasePrice())
                .sellingPrice(doc.getSellingPrice())
                .active(doc.isActive())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
