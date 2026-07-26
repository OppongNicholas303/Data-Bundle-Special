package com.space.space_bundle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BotPurchaseResponse {
    private String status;
    private String message;
    private Integer code;
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    @JsonProperty("order_status")
    private String orderStatus;
    
    private Double amount;
}
