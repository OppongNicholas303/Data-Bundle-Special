package com.space.space_bundle.core.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import java.math.BigDecimal;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bundle_prices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BundlePrice {

    @Id
    private Long packageId;

    private BigDecimal sellingPrice;
    private String name;

}
