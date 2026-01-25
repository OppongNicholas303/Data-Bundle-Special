package com.space.space_bundle.in.web;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.services.OrderService;
import com.space.space_bundle.in.web.dto.PlaceOrderRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order placeOrder(@RequestBody PlaceOrderRequest request) throws Exception {
        Bundle bundle = new Bundle(
                request.getBundleCode(),
                request.getDataSize(),
                request.getCostPrice(),
                request.getSellingPrice()
        );

        return orderService.placeOrder(
                UUID.fromString(request.getUserId()),
                request.getRecipientPhone(),
                bundle
        );
    }
}

