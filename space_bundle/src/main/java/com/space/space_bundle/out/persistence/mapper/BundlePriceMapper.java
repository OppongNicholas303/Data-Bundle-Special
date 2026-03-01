package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.BundlePrice;
import com.space.space_bundle.out.persistence.entity.BundlePriceDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BundlePriceMapper {

    public static BundlePriceDocument toDocument(BundlePrice domain) {
        return BundlePriceDocument.builder()
                .packageId(domain.getPackageId())
                .sellingPrice(domain.getSellingPrice())
                .name(domain.getName())
                .build();
    }

    public static BundlePrice toDomain(BundlePriceDocument doc) {
        return BundlePrice.builder()
                .packageId(doc.getPackageId())
                .sellingPrice(doc.getSellingPrice())
                .name(doc.getName())
                .build();
    }
}

