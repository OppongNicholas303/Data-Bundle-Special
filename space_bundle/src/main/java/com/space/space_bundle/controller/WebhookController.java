package com.space.space_bundle.controller;

import com.space.space_bundle.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping({"/webhook/paystack", "/webhooks/paystack"})
    public ResponseEntity<Void> paystack(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            HttpServletRequest request) {

        System.out.println("recieve");

        if (signature == null || signature.isBlank()) {
            log.warn("[WEBHOOK] Rejected — missing x-paystack-signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!webhookService.isValidSignature(payload, signature)) {
            log.warn("[WEBHOOK] Rejected — invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[WEBHOOK] Received valid webhook from Paystack");
        log.info("[WEBHOOK] Signature verified, processing");
        webhookService.processPaystack(payload);
        return ResponseEntity.ok().build();
    }
}
