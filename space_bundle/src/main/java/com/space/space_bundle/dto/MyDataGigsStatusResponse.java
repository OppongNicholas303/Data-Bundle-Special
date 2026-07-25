package com.space.space_bundle.dto;

import lombok.Data;

@Data
public class MyDataGigsStatusResponse {
    private String status;
    private Long order_id;
    private String order_status;
    private Double amount;
}
