package com.space.space_bundle.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopUpResponse {
    private String topUpId;
    private String reference;
    private String authorizationUrl;
    private String accessCode;
}
