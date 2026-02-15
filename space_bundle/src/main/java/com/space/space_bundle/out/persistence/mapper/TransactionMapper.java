package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.out.persistence.entity.TransactionDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TransactionMapper {

    public static TransactionDocument toDocument(Transaction transaction) {
        return TransactionDocument.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .orderId(transaction.getOrderId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    public static Transaction toDomain(TransactionDocument doc) {
        return Transaction.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .orderId(doc.getOrderId())
                .type(Transaction.TransactionType.valueOf(doc.getType()))
                .amount(doc.getAmount())
                .currency(doc.getCurrency())
                .status(Transaction.TransactionStatus.valueOf(doc.getStatus()))
                .reference(doc.getReference())
                .description(doc.getDescription())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}