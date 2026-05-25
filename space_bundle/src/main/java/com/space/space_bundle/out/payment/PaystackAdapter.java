package com.space.space_bundle.out.payment;

import com.space.space_bundle.out.payment.dto.PaystackInitializeResponse;
import com.space.space_bundle.out.payment.dto.PaystackVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.util.retry.Retry;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PaystackAdapter {

    @Value("${paystack.secret-key}")
    private String secretKey;

    private final WebClient webClient;

    public PaystackAdapter() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.paystack.co")
                .build();
    }

    public PaystackInitializeResponse initializeTransaction(String email, Integer amount, String reference, String callbackUrl) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("amount", amount);
        requestBody.put("reference", reference);
        requestBody.put("callback_url", callbackUrl);
        requestBody.put("channels", new String[]{"mobile_money", "card"}); // 👈 ADD THIS

        log.info("Initializing Paystack transaction: email={}, amount={}, reference={}", email, amount, reference);

        try {
            PaystackInitializeResponse response = webClient.post()
                    .uri("/transaction/initialize")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(PaystackInitializeResponse.class)
                    .timeout(Duration.ofSeconds(15))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                    .block();
            
            log.info("Paystack response: status={}, authUrl={}, accessCode={}, reference={}", 
                    response.isStatus(), 
                    response.getData() != null ? response.getData().getAuthorization_url() : null,
                    response.getData() != null ? response.getData().getAccess_code() : null,
                    response.getData() != null ? response.getData().getReference() : null);
            
            return response;
        } catch (Exception e) {
            log.error("Paystack API error: {}", e.getMessage(), e);
            throw e;
        }
    }

    public PaystackVerifyResponse verifyTransaction(String reference) {
        log.info("Verifying Paystack transaction: reference={}", reference);

        return webClient.get()
                .uri("/transaction/verify/" + reference)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .retrieve()
                .bodyToMono(PaystackVerifyResponse.class)
                .timeout(Duration.ofSeconds(15))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .block();
    }

    /**
     * Create a Paystack transfer recipient (for agent withdrawals)
     */
    public String createTransferRecipient(String name, String bankCode, String accountNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "ghipss");  // Ghana Interbank Payment and Settlement System
        body.put("name", name);
        body.put("account_number", accountNumber);
        body.put("bank_code", bankCode);
        body.put("currency", "GHS");

        log.info("Creating Paystack transfer recipient: name={}, bankCode={}", name, bankCode);

        try {
            Map response = webClient.post()
                    .uri("/transferrecipient")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
                throw new RuntimeException("Failed to create transfer recipient");
            }
            Map data = (Map) response.get("data");
            return (String) data.get("recipient_code");
        } catch (Exception e) {
            log.error("Paystack create recipient error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create transfer recipient: " + e.getMessage());
        }
    }

    /**
     * Initiate a Paystack transfer (for agent withdrawals)
     */
    public Map<String, Object> initiateTransfer(int amountInKobo, String recipientCode,
                                                  String reference, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("source", "balance");
        body.put("amount", amountInKobo);
        body.put("recipient", recipientCode);
        body.put("reference", reference);
        body.put("reason", reason);
        body.put("currency", "GHS");

        log.info("Initiating Paystack transfer: reference={}, amount={}", reference, amountInKobo);

        try {
            Map response = webClient.post()
                    .uri("/transfer")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
                throw new RuntimeException("Paystack transfer failed");
            }
            return (Map<String, Object>) response.get("data");
        } catch (Exception e) {
            log.error("Paystack transfer error: {}", e.getMessage(), e);
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }
}
