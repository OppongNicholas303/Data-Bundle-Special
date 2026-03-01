package com.space.space_bundle.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "bundle_prices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BundlePriceDocument {
    @Id
    private Long packageId;
    private BigDecimal sellingPrice;
    private String name;
}
