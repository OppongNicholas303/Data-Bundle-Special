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

        log.info("[WEBHOOK] Received Moolre webhook payload: {}", payload);

        if (!webhookService.isValidMoolreWebhook(payload)) {
            log.warn("[WEBHOOK] Rejected — invalid secret or payload");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[WEBHOOK] Secret verified, processing");
        webhookService.processMoolre(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping({"/webhook/checkerport", "/webhooks/checkerport"})
    public ResponseEntity<Void> checkerport(
            @RequestBody java.util.Map<String, Object> payload,
            @RequestHeader(value = "x-api-key", required = false) String apiKey) {
        
        log.info("[WEBHOOK] Received CheckerPort webhook payload: {}", payload);

        String cleanExpected = checkerportApiKey != null ? checkerportApiKey.replace("\"", "").trim() : "";
        String cleanReceived = apiKey != null ? apiKey.replace("\"", "").trim() : "";

        if (cleanReceived.isEmpty() || !cleanReceived.equals(cleanExpected)) {
            log.warn("[WEBHOOK] Rejected CheckerPort webhook — invalid or missing API key. Received: '{}', Expected: '{}'", apiKey, checkerportApiKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[WEBHOOK] API key verified, processing CheckerPort webhook");
        resultsCheckerService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
