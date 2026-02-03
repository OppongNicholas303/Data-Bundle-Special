package com.space.space_bundle.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}