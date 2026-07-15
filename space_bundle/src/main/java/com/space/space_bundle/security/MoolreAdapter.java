package com.space.space_bundle.security;

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
public class MoolreAdapter {

    @Value("${moolre.api-user}")
    private String apiUser;

    @Value("${moolre.pub-key}")
    private String pubKey;

    @Value("${moolre.account-number}")
    private String accountNumber;

    private final WebClient webClient;

    public MoolreAdapter() {
        this.webClient = WebClient.builder().baseUrl("https://api.moolre.com").build();
    }

    /**
     * Generates a Moolre payment link for a transaction.
     */
    public Map<String, Object> generatePaymentLink(Double amount, String email, String externalRef,
                                                   String callbackUrl, String redirectUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", 1);
        body.put("amount", amount);
        body.put("currency", "GHS");
        body.put("accountnumber", accountNumber);
        body.put("email", email);
        body.put("externalref", externalRef);
        
        if (callbackUrl != null) body.put("callback", callbackUrl);
        if (redirectUrl != null) body.put("redirect", redirectUrl);
        
        body.put("reusable", false);

        log.info("Moolre init: email={}, amount={}, ref={}", email, amount, externalRef);
        
        Map response = webClient.post().uri("/embed/link")
                .header("X-API-USER", apiUser)
                .header("X-API-PUBKEY", pubKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Moolre API Error: status={}, body={}", clientResponse.statusCode(), errorBody);
                                    return reactor.core.publisher.Mono.error(new RuntimeException("Moolre API Error: " + errorBody));
                                }))
                .bodyToMono(String.class)
                .map(bodyStr -> {
                    try {
                        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(bodyStr, Map.class);
                    } catch (Exception e) {
                        log.error("Failed to parse Moolre init response. Body: {}", bodyStr);
                        throw new RuntimeException("Moolre response parse error", e);
                    }
                })
                .timeout(Duration.ofSeconds(15))
                .block();

        if (response == null || !Integer.valueOf(1).equals(response.get("status"))) {
            String msg = response != null ? String.valueOf(response.get("message")) : "No response";
            throw new RuntimeException("Moolre init failed: " + msg);
        }
        return (Map<String, Object>) response.get("data");
    }

    /**
     * Checks the status of a Moolre transaction manually.
     */
    public Map<String, Object> checkPaymentStatus(String externalRef) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", 1);
        body.put("idtype", 1); // 1 = externalref
        body.put("id", externalRef);
        body.put("accountnumber", accountNumber);

        log.info("Moolre check status: ref={}", externalRef);

        Map response = webClient.post().uri("/open/transact/status")
                .header("X-API-USER", apiUser)
                .header("X-API-PUBKEY", pubKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(bodyStr -> {
                    try {
                        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(bodyStr, Map.class);
                    } catch (Exception e) {
                        log.error("Failed to parse Moolre status response. Body: {}", bodyStr);
                        throw new RuntimeException("Moolre response parse error", e);
                    }
                })
                .timeout(Duration.ofSeconds(15))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .block();

        if (response == null || !Integer.valueOf(1).equals(response.get("status"))) {
            String msg = response != null ? String.valueOf(response.get("message")) : "No response";
            throw new RuntimeException("Moolre status check failed: " + msg);
        }
        return (Map<String, Object>) response.get("data");
    }
}
