package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.out.automation.MyDataGigsBotService;
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

    private final MyDataGigsBotService botService;

    @GetMapping("/track-order/{phoneNumber}")
    public ResponseEntity<MyDataGigsBotService.OrderStatus> trackOrder(@PathVariable String phoneNumber) {
        MyDataGigsBotService.OrderStatus status = botService.trackOrder(phoneNumber);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
