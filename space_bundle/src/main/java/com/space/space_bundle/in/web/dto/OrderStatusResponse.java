package com.space.space_bundle.in.web.dto;

public record OrderStatusResponse(
        String orderNumber,
        String customerPhone,
        String status,
        String packageName,
        String packageSize,
        String packageNetwork
) {}
