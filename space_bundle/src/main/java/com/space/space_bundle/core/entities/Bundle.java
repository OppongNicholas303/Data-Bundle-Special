package com.space.space_bundle.core.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Bundle {
    private String id;
    private String code;
    private String name;
    private String dataSize;
    private String network;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BundleStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum BundleStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK
    }

    public BigDecimal getProfit() {
        return sellingPrice.subtract(costPrice);
    }

    public void activate() {
        this.status = BundleStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = BundleStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void markOutOfStock() {
        this.status = BundleStatus.OUT_OF_STOCK;
        this.updatedAt = LocalDateTime.now();
    }
}