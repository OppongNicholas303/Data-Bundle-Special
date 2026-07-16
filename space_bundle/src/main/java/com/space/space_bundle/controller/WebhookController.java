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
    private final com.space.space_bundle.service.ResultsCheckerService resultsCheckerService;

    @org.springframework.beans.factory.annotation.Value("${checkerport.api-key}")
    private String checkerportApiKey;

    @PostMapping({"/webhook/moolre", "/webhooks/moolre"})
    public ResponseEntity<Void> moolre(
            @RequestBody String payload,
            HttpServletRequest request) {

        System.out.println("receive moolre webhook");

        if (!webhookService.isValidMoolreWebhook(payload)) {
            log.warn("[WEBHOOK] Rejected — invalid secret or payload");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[WEBHOOK] Received valid webhook from Moolre");
        log.info("[WEBHOOK] Secret verified, processing");
        webhookService.processMoolre(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping({"/webhook/checkerport", "/webhooks/checkerport"})
    public ResponseEntity<Void> checkerport(
            @RequestBody java.util.Map<String, Object> payload,
            @RequestHeader(value = "x-api-key", required = false) String apiKey) {
        
        if (apiKey == null || !apiKey.equals(checkerportApiKey)) {
            log.warn("[WEBHOOK] Rejected CheckerPort webhook — invalid or missing API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[WEBHOOK] Received valid webhook from CheckerPort");
        resultsCheckerService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
