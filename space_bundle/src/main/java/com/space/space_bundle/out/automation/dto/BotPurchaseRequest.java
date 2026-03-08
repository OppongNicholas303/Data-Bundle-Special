package com.space.space_bundle.out.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BotPurchaseRequest {
    private String network;
    private String beneficiary;
    
    @JsonProperty("pa_data-bundle-packages")
    private Integer dataBundlePackages;

    private String package_id;
    private String customer_phone;

}