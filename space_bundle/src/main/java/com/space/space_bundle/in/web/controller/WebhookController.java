package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.services.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentWebhookService webhookService;

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    @PostMapping("/paystack")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {

        log.info("Received Paystack webhook");
        log.info("Payload: {}", payload);

        // Verify signature
        if (!verifySignature(payload, signature)) {
            log.error("Invalid webhook signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }


        log.info("Webhook payload: {}", payload);
        log.info("in the controller");
        try {
            webhookService.processPaystackWebhook(payload);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Processing failed");
        }
    }

    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
