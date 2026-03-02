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
                    .retry(3)
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
                .block();
    }
}
