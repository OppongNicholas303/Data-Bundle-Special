package com.space.space_bundle.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBundleRequest {
    private String code;
    private String name;
    private String dataSize;
    private String network;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String description;
    private String preferredProvider;
}
