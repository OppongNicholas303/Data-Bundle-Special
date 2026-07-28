package com.space.space_bundle.dto;

import com.space.space_bundle.entity.Bundle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBundleResponse {
    private String id;
    private String code;
    private String name;
    private String dataSize;
    private String network;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String status;
    private String description;
    private String preferredProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminBundleResponse from(Bundle bundle) {
        if (bundle == null) return null;
        return AdminBundleResponse.builder()
                .id(bundle.getId())
                .code(bundle.getCode())
                .name(bundle.getName())
                .dataSize(bundle.getDataSize())
                .network(bundle.getNetwork())
                .costPrice(bundle.getCostPrice())
                .sellingPrice(bundle.getSellingPrice())
                .status(bundle.getStatus())
                .description(bundle.getDescription())
                .preferredProvider(bundle.getPreferredProvider())
                .createdAt(bundle.getCreatedAt())
                .updatedAt(bundle.getUpdatedAt())
                .build();
    }
}
