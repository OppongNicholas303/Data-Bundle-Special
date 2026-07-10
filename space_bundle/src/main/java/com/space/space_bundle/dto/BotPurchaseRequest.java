package com.space.space_bundle.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotPurchaseRequest {
    private String network;
    private String beneficiary;
    private Integer dataBundlePackages;
    private Integer package_id;
    private String customer_phone;
}
