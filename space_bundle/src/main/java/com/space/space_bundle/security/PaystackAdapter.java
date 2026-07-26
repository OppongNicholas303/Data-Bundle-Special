package com.space.space_bundle.security;

import com.space.space_bundle.dto.PaystackInitializeResponse;
import com.space.space_bundle.dto.PaystackVerifyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PaystackAdapter {

    @Value("${paystack.secret-key}")
    private String secretKey;

    private final WebClient webClient;

    public PaystackAdapter() {
        this.webClient = WebClient.builder().baseUrl("https://api.paystack.co").build();
    }

    public PaystackInitializeResponse initializeTransaction(String email, Integer amount,
                                                             String reference, String callbackUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("amount", amount);
        body.put("reference", reference);
        body.put("callback_url", callbackUrl);
        body.put("channels", new String[]{"mobile_money", "card"});

        log.info("Paystack init: email={}, amount={}, ref={}", email, amount, reference);
        String trimmedKey = secretKey == null ? "" : secretKey.trim();
        log.info("Using secret key of length: {}", trimmedKey.length());
        return webClient.post().uri("/transaction/initialize")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + trimmedKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body).retrieve()
                .bodyToMono(PaystackInitializeResponse.class)
                .timeout(Duration.ofSeconds(15))
                .doOnError(e -> log.error("Paystack API error: {}", e.getMessage()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .block();
    }

    public PaystackVerifyResponse verifyTransaction(String reference) {
        log.info("Paystack verify: ref={}", reference);
        String trimmedKey = secretKey == null ? "" : secretKey.trim();
        return webClient.get().uri("/transaction/verify/" + reference)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + trimmedKey)
                .retrieve().bodyToMono(PaystackVerifyResponse.class)
                .timeout(Duration.ofSeconds(15))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .block();
    }

    public String createTransferRecipient(String name, String bankCode, String accountNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "ghipss");
        body.put("name", name);
        body.put("account_number", accountNumber);
        body.put("bank_code", bankCode);
        body.put("currency", "GHS");

        Map response = webClient.post().uri("/transferrecipient")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body).retrieve().bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(15)).block();

        if (response == null || !Boolean.TRUE.equals(response.get("status")))
            throw new RuntimeException("Failed to create transfer recipient");
        return (String) ((Map) response.get("data")).get("recipient_code");
    }

    /**
     * Create a Paystack MoMo transfer recipient for Ghana mobile money.
     * momoProvider: "mtn", "vodafone", "airteltigo"
     * Paystack bank codes for Ghana MoMo:
     *   MTN Mobile Money  -> "MTN"
     *   Vodafone Cash     -> "VOD"
     *   AirtelTigo Money  -> "ATL"
     */
    public String createMomoRecipient(String accountName, String momoProvider, String momoNumber) {
        String bankCode = resolveMomoBankCode(momoProvider);

        Map<String, Object> body = new HashMap<>();
        body.put("type", "mobile_money");
        body.put("name", accountName);
        body.put("account_number", momoNumber);
        body.put("bank_code", bankCode);
        body.put("currency", "GHS");

        log.info("Creating MoMo recipient: provider={}, bankCode={}, number={}", momoProvider, bankCode, momoNumber);

        Map response = webClient.post().uri("/transferrecipient")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body).retrieve().bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(15)).block();

        if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
            String msg = response != null ? String.valueOf(response.get("message")) : "No response";
            throw new RuntimeException("Failed to create MoMo recipient: " + msg);
        }
        return (String) ((Map) response.get("data")).get("recipient_code");
    }

    private String resolveMomoBankCode(String provider) {
        return switch (provider.toUpperCase()) {
            case "MTN"                    -> "MTN";
            case "VODAFONE", "TELECEL"    -> "VOD";
            case "AIRTELTIGO"             -> "ATL";
            default -> throw new IllegalArgumentException("Unsupported MoMo provider: " + provider);
        };
    }

    public Map<String, Object> initiateTransfer(int amountInPesewas, String recipientCode,
                                                  String reference, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("source", "balance");
        body.put("amount", amountInPesewas);
        body.put("recipient", recipientCode);
        body.put("reference", reference);
        body.put("reason", reason);
        body.put("currency", "GHS");

        Map response = webClient.post().uri("/transfer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body).retrieve().bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(15)).block();

        if (response == null || !Boolean.TRUE.equals(response.get("status")))
            throw new RuntimeException("Paystack transfer failed");
        return (Map<String, Object>) response.get("data");
    }
}
