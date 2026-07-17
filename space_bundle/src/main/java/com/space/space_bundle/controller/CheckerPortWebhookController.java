package com.space.space_bundle.controller;

import com.space.space_bundle.service.ResultsCheckerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/results-checker/webhooks")
@RequiredArgsConstructor
public class CheckerPortWebhookController {

    private final ResultsCheckerService resultsCheckerService;

    @Value("${checkerport.api-key}")
    private String configuredApiKey;

    @PostMapping("/checkerport")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("x-api-key") String apiKey,
            @RequestBody Map<String, Object> payload) {

        if (!configuredApiKey.equals(apiKey)) {
            log.warn("Unauthorized webhook attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            resultsCheckerService.handleWebhook(payload);
        } catch (Exception e) {
            log.error("Error processing CheckerPort webhook", e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
