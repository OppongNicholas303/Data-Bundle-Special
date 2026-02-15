package com.space.space_bundle.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpResponse {
    private String topUpId;
    private String reference;
    private String authorizationUrl;
    private String accessCode;
}
