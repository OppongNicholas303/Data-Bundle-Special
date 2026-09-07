package com.space.space_bundle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessDataOrderRequest {
    private String phone;    // Ghana number without +233 or 233, e.g. "0241234567"
    private Integer size;    // bundle size in GB as integer, e.g. 1
    private String network;  // MTN, TELECEL, AIRTELTIGO_ISHARE, AIRTELTIGO_BIGTIME
    private String callback; // optional webhook URL
}
