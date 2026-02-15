package com.space.space_bundle.out.payment.dto;

import lombok.Data;

@Data
public class PaystackVerifyResponse {
    private boolean status;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String reference;
        private String status;
        private Integer amount;
        private String currency;
        private String paid_at;
    }
}
