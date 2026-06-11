package com.space.space_bundle.dto;

import com.space.space_bundle.entity.MashupBundle;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MashupPackageResponse {
    private String id;
    private Integer packageId;
    private Integer specialOfferPackageId;
    private String slug;
    private String code;
    private String name;
    private String description;
    private BigDecimal dataAmountMb;
    private String dataSize;
    private String network;
    private BigDecimal sellingPrice;
    private String status;
    private boolean available;
    private LocalDateTime lastSeenAt;

    public static MashupPackageResponse from(MashupBundle bundle) {
        return MashupPackageResponse.builder()
                .id(bundle.getId())
                .packageId(bundle.getSpecialOfferPackageId())
                .specialOfferPackageId(bundle.getSpecialOfferPackageId())
                .slug(bundle.getSlug())
                .code(bundle.getSlug())
                .name(bundle.getName())
                .description(bundle.getDescription())
                .dataAmountMb(bundle.getDataAmountMb())
                .dataSize(bundle.getDataSize())
                .network(bundle.getNetwork())
                .sellingPrice(bundle.getSellingPrice())
                .status(bundle.getStatus())
                .available(bundle.isAvailable())
                .lastSeenAt(bundle.getLastSeenAt())
                .build();
    }
}
