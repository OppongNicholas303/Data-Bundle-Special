package com.space.space_bundle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mashup_bundles")
@CompoundIndex(name = "mashup_package_unique", def = "{'specialOfferPackageId': 1}", unique = true)
public class MashupBundle {

    @Id
    private String id;

    private Integer externalId;
    private Integer specialOfferPackageId;
    private String slug;
    private String name;
    private String description;
    private BigDecimal dataAmountMb;
    private String dataSize;
    private String network;

    @JsonIgnore
    private BigDecimal costPrice;

    private BigDecimal sellingPrice;
    private String status;
    private boolean available;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum MashupStatus {
        ACTIVE, INACTIVE
    }

    public boolean isPurchasable() {
        return available
                && MashupStatus.ACTIVE.name().equals(status)
                && sellingPrice != null
                && sellingPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}
