package com.space.space_bundle.out.automation.dto;

import lombok.Data;
public record BotPurchaseResponseRandy(
        boolean success,
        String message,
        Order order
) {}