package com.space.space_bundle.in.web.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private String userId;
    private String recipientPhone;
    private String bundleCode;
    private String dataSize;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
}