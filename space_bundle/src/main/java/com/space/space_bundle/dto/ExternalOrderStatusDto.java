package com.space.space_bundle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalOrderStatusDto {
    private String provider;
    private String status;
    private String providerOrderId;
    private String amount;
}
