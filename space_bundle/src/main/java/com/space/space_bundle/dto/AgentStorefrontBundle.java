package com.space.space_bundle.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AgentStorefrontBundle {
    private String bundleId;
    private String bundleCode;
    private String name;
    private String dataSize;
    private String network;
    private BigDecimal sellingPrice;
    private String agentCode;
    private String bundleType; // "STANDARD" or "MASHUP"
}
