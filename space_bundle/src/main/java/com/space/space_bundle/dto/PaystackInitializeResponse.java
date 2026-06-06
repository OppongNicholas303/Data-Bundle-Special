package com.space.space_bundle.dto;

import lombok.Data;

@Data
public class PaystackInitializeResponse {
    private boolean status;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String authorization_url;
        private String access_code;
        private String reference;
    }
}
