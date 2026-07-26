package com.space.space_bundle.dto;

import lombok.Data;

@Data
public class PaystackVerifyResponse {
    private boolean status;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String status;
        private String reference;
        private Integer amount;
        private String gateway_response;
        private String paid_at;
        private String created_at;
        private String channel;
        private String currency;
        private String ip_address;
    }
}
