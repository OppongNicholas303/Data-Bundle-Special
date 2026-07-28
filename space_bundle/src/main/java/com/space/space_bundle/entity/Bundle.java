package com.space.space_bundle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bundles")
public class Bundle {

    @Id
    private String id;
    private String code;
    private String name;
    private String dataSize;
    private String network;

    @JsonIgnore  // Internal cost — never expose to clients
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String status;
    private String description;
    private String preferredProvider; // "ramdy", "mydatagigs", etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum BundleStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK
    }
}
