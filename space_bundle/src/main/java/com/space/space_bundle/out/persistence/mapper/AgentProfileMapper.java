package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.AgentProfile;
import com.space.space_bundle.out.persistence.entity.AgentProfileDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AgentProfileMapper {

    public static AgentProfileDocument toDocument(AgentProfile profile) {
        return AgentProfileDocument.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .businessName(profile.getBusinessName())
                .referralCode(profile.getReferralCode())
                .totalSales(profile.getTotalSales())
                .totalProfit(profile.getTotalProfit())
                .active(profile.isActive())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public static AgentProfile toDomain(AgentProfileDocument doc) {
        return AgentProfile.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .businessName(doc.getBusinessName())
                .referralCode(doc.getReferralCode())
                .totalSales(doc.getTotalSales())
                .totalProfit(doc.getTotalProfit())
                .active(doc.isActive())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
