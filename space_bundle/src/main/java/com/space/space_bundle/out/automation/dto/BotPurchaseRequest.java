package com.space.space_bundle.out.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BotPurchaseRequest {
    private int package_id;
    private String customer_phone;

}