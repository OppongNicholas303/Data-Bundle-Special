package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.out.persistence.entity.BundleDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BundleMapper {

    public static BundleDocument toDocument(Bundle bundle) {
        return BundleDocument.builder()
                .id(bundle.getId())
                .code(bundle.getCode())
                .name(bundle.getName())
                .dataSize(bundle.getDataSize())
                .network(bundle.getNetwork())
                .costPrice(bundle.getCostPrice())
                .sellingPrice(bundle.getSellingPrice())
                .status(bundle.getStatus().name())
                .description(bundle.getDescription())
                .createdAt(bundle.getCreatedAt())
                .updatedAt(bundle.getUpdatedAt())
                .build();
    }

    public static Bundle toDomain(BundleDocument doc) {
        return Bundle.builder()
                .id(doc.getId())
                .code(doc.getCode())
                .name(doc.getName())
                .dataSize(doc.getDataSize())
                .network(doc.getNetwork())
                .costPrice(doc.getCostPrice())
                .sellingPrice(doc.getSellingPrice())
                .status(Bundle.BundleStatus.valueOf(doc.getStatus()))
                .description(doc.getDescription())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}