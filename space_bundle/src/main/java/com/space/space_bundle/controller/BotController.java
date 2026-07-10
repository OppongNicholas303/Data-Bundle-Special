package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    private final OrderRepository orderRepository;

    @GetMapping("/track-order/{phoneNumber}")
    public ResponseEntity<ApiResponse<Order>> trackOrder(@PathVariable String phoneNumber) {
        return orderRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("No recent order found for this number")));
    }
}
