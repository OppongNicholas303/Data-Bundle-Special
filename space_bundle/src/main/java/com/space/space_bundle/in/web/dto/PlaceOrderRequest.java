package com.space.space_bundle.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private String network;
    private String phoneNumber;
    private String bundleCode;
    private String email;      // For guest users
    private String package_id;
    private String agentCode;  // Optional: agent's referral code from storefront link e.g. "AGT-X7K2A"
}