package com.space.space_bundle.out.automation.dto;

import lombok.Data;


public record BotPurchaseResponse(
        boolean success,
        String message,
        Order order
) {}