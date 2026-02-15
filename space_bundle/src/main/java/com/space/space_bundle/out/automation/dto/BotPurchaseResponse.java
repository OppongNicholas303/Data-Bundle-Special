package com.space.space_bundle.out.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BotPurchaseResponse {
    private String status;
    private String code;
    private String message;
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    private Double amount;
    private String network;
    private String beneficiary;
}