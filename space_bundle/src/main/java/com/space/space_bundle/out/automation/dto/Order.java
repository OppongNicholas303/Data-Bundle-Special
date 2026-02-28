package com.space.space_bundle.out.automation.dto;

import lombok.Data;

import java.time.Instant;

//@Data
public record Order(
        Long id,
        String order_number,
        String customer_phone,
        String status,
        String package_name,
        String package_size,
        String package_network,
        String cost_price,
        Instant created_at,
        Instant updated_at
) {}