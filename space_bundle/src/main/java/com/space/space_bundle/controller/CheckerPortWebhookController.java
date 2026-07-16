package com.space.space_bundle.controller;

import com.space.space_bundle.dto.checkerport.CheckerPortResponse;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.entity.ServiceStatus;
import com.space.space_bundle.repository.ResultsTransactionRepository;
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

    private final ResultsTransactionRepository transactionRepository;

    @Value("${checkerport.api-key}")
    private String configuredApiKey;

    @PostMapping("/checkerport")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("x-api-key") String apiKey,
            @RequestBody CheckerPortResponse<Map<String, Object>> payload) {

        if (!configuredApiKey.equals(apiKey)) {
            log.warn("Unauthorized webhook attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (payload.getData() == null || !payload.getData().containsKey("referenceId")) {
            log.error("Webhook payload missing referenceId");
            return ResponseEntity.badRequest().build();
        }

        String referenceId = (String) payload.getData().get("referenceId");

        Optional<ResultsTransaction> txOpt = transactionRepository.findByReferenceId(referenceId);
        if (txOpt.isEmpty()) {
            log.warn("Webhook received for unknown referenceId: {}", referenceId);
            return ResponseEntity.ok().build(); // Return 200 so they stop retrying
        }

        ResultsTransaction tx = txOpt.get();

        // Idempotency: if already completed/failed and webhook received, skip
        if (tx.isWebhookReceived() && (tx.getStatus() == ServiceStatus.COMPLETE || tx.getStatus() == ServiceStatus.FAILED)) {
            return ResponseEntity.ok().build();
        }

        // Map status
        ServiceStatus newStatus = mapServiceStatus(payload.getData().get("serviceStatus"));
        if (newStatus == null) {
            newStatus = "FAILED".equalsIgnoreCase(payload.getStatus()) ? ServiceStatus.FAILED : tx.getStatus();
        }

        tx.setStatus(newStatus);
        tx.setWebhookReceived(true);
        tx.setMessage(payload.getMessage());
        tx.setErrorCode(payload.getErrorCode());
        
        if (payload.getData().containsKey("statusCode")) {
            tx.setStatusCode((String) payload.getData().get("statusCode"));
        }

        if (payload.getData().containsKey("result")) {
            tx.setResultData((Map<String, Object>) payload.getData().get("result"));
        }
        if (payload.getData().containsKey("vouchers")) {
            tx.setVouchers(payload.getData().get("vouchers"));
        }

        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        return ResponseEntity.ok().build();
    }

    private ServiceStatus mapServiceStatus(Object statusObj) {
        if (statusObj == null) return null;
        String statusStr = statusObj.toString();
        switch (statusStr.toLowerCase()) {
            case "pending": return ServiceStatus.PENDING;
            case "pending-input": return ServiceStatus.PENDING_INPUT;
            case "complete": return ServiceStatus.COMPLETE;
            default: return null;
        }
    }
}
