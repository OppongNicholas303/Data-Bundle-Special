package com.space.space_bundle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotPurchaseRequest {
    private String network;
    private String beneficiary;
    @JsonProperty("pa_data-bundle-packages")
    private Integer dataBundlePackages;
    private Integer package_id;
    private String customer_phone;
}
