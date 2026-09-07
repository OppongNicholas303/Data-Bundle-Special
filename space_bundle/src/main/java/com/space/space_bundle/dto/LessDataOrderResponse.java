package com.space.space_bundle.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessDataOrderResponse {

    private boolean status;
    private int statusCode;
    private String message;
    private Payload payload;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private String id;          // LessData order UUID — used for status checks
        private String orderCode;   // e.g. "#00044"
        private String phone;
        private Integer size;
        private Integer price;
        private String network;
        private String source;
        private String status;      // PENDING, COMPLETED, FAILED
        private String externalRef;
        private String callback;
        private String createdAt;
        private String updatedAt;
    }
}
