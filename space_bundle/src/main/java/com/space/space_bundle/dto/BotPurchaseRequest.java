package com.space.space_bundle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotPurchaseRequest {
    private String network;
    private String beneficiary;
    @JsonProperty("pa_data-bundle-packages")
    private Integer dataBundlePackages;
    private Integer package_id;
    private String customer_phone;
}
