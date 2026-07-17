package com.space.space_bundle.service;

import com.space.space_bundle.dto.MoolreSmsMessage;
import com.space.space_bundle.dto.MoolreSmsRequest;
import com.space.space_bundle.dto.MoolreSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MoolreSmsService {

    @Value("${moolre.sms.api-key:YOUR_X_API_VASKEY}")
    private String apiKey;

    @Value("${moolre.sms.api-url:https://api.moolre.com/open/sms/send}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public MoolreSmsService() {
        this.restTemplate = new RestTemplate();
    }

    public List<String> sendBulkSms(String senderId, String message, List<String> recipients) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-VASKEY", apiKey);
        headers.set("Content-Type", "application/json");

        List<MoolreSmsMessage> messages = new ArrayList<>();
        List<String> refs = new ArrayList<>();

        for (String recipient : recipients) {
            String ref = UUID.randomUUID().toString();
            refs.add(ref);
            messages.add(MoolreSmsMessage.builder()
                    .recipient(recipient)
                    .message(message)
                    .ref(ref)
                    .build());
        }

        MoolreSmsRequest request = MoolreSmsRequest.builder()
                .type(1)
                .senderid(senderId)
                .messages(messages)
                .build();

        HttpEntity<MoolreSmsRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<MoolreSmsResponse> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, MoolreSmsResponse.class);

            MoolreSmsResponse body = response.getBody();
            if (body != null && body.getStatus() == 1) {
                log.info("Successfully dispatched SMS batch to Moolre API");
                return refs;
            } else {
                log.error("Moolre SMS API failed: {}", body != null ? body.getMessage() : "Unknown error");
                throw new RuntimeException("SMS Dispatch failed: " + (body != null ? body.getMessage() : "No response"));
            }
        } catch (Exception e) {
            log.error("Exception dispatching SMS to Moolre", e);
            throw new RuntimeException("Failed to send SMS via Moolre API", e);
        }
    }

    public static int calculatePages(String message) {
        boolean isUnicode = message.chars().anyMatch(c -> c > 127);
        int length = message.length();
        if (isUnicode) {
            if (length <= 70) return 1;
            return (int) Math.ceil(length / 67.0);
        } else {
            if (length <= 160) return 1;
            return (int) Math.ceil(length / 153.0);
        }
    }
}
