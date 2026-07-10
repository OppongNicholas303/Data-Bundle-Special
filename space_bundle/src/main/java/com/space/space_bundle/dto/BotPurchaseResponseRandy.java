package com.space.space_bundle.dto;

public record BotPurchaseResponseRandy(
        boolean success,
        String message,
        OrderData order
) {
    public record OrderData(
            Long id,
            String order_number,
            String status
    ) {}
}
