package com.space.space_bundle.dto;

import com.space.space_bundle.entity.MashupBundle;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminMashupPackageResponse {
    private String id;
    private Integer externalId;
    private Integer packageId;
    private Integer specialOfferPackageId;
    private String slug;
    private String name;
    private String description;
    private BigDecimal dataAmountMb;
    private String dataSize;
    private String network;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String status;
    private boolean available;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminMashupPackageResponse from(MashupBundle bundle) {
        return AdminMashupPackageResponse.builder()
                .id(bundle.getId())
                .externalId(bundle.getExternalId())
                .packageId(bundle.getSpecialOfferPackageId())
                .specialOfferPackageId(bundle.getSpecialOfferPackageId())
                .slug(bundle.getSlug())
                .name(bundle.getName())
                .description(bundle.getDescription())
                .dataAmountMb(bundle.getDataAmountMb())
                .dataSize(bundle.getDataSize())
                .network(bundle.getNetwork())
                .costPrice(bundle.getCostPrice())
                .sellingPrice(bundle.getSellingPrice())
                .status(bundle.getStatus())
                .available(bundle.isAvailable())
                .lastSeenAt(bundle.getLastSeenAt())
                .createdAt(bundle.getCreatedAt())
                .updatedAt(bundle.getUpdatedAt())
                .build();
    }
}
