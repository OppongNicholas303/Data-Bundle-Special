package com.space.space_bundle.dto;

import lombok.Data;

@Data
public class BotPurchaseResponse {
    private String status;
    private String message;
    private Integer code;
    private Integer orderId;
}
