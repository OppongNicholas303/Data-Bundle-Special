package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.out.persistence.entity.OrderDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrderMapper {

    public static OrderDocument toDocument(Order order) {
        return OrderDocument.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .network(order.getNetwork())
                .phoneNumber(order.getPhoneNumber())
                .bundleCode(order.getBundleCode())
                .amount(order.getAmount())
                .status(order.getStatus().name())
//                .providerReference(order.getProviderOrderNumber())
                .paymentReference(order.getPaymentReference())
                .paymentUrl(order.getPaymentUrl())
                .paymentAccessCode(order.getPaymentAccessCode())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public static Order toDomain(OrderDocument doc) {
        return Order.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .network(doc.getNetwork())
                .phoneNumber(doc.getPhoneNumber())
                .bundleCode(doc.getBundleCode())
                .amount(doc.getAmount())
                .status(com.space.space_bundle.core.enums.OrderStatus.valueOf(doc.getStatus()))
//                .providerReference(doc.getProviderReference())
                .paymentReference(doc.getPaymentReference())
                .paymentUrl(doc.getPaymentUrl())
                .paymentAccessCode(doc.getPaymentAccessCode())
                .failureReason(doc.getFailureReason())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
