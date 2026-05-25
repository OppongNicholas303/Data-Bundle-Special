package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.Commission;
import com.space.space_bundle.out.persistence.entity.CommissionDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommissionMapper {

    public static CommissionDocument toDocument(Commission commission) {
        return CommissionDocument.builder()
                .id(commission.getId())
                .agentId(commission.getAgentId())
                .orderId(commission.getOrderId())
                .baseAmount(commission.getBaseAmount())
                .sellingAmount(commission.getSellingAmount())
                .profit(commission.getProfit())
                .status(commission.getStatus().name())
                .createdAt(commission.getCreatedAt())
                .build();
    }

    public static Commission toDomain(CommissionDocument doc) {
        return Commission.builder()
                .id(doc.getId())
                .agentId(doc.getAgentId())
                .orderId(doc.getOrderId())
                .baseAmount(doc.getBaseAmount())
                .sellingAmount(doc.getSellingAmount())
                .profit(doc.getProfit())
                .status(Commission.CommissionStatus.valueOf(doc.getStatus()))
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
