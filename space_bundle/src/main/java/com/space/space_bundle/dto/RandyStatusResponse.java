package com.space.space_bundle.dto;

import lombok.Data;

@Data
public class RandyStatusResponse {
    private boolean success;
    private String error;
    private RandyOrder order;

    @Data
    public static class RandyOrder {
        private Long id;
        private String order_number;
        private String customer_phone;
        private String status;
        private String package_name;
        private String package_size;
        private String package_network;
        private String branch_name;
        private String cost_price;
        private String created_at;
        private String updated_at;
    }
}
