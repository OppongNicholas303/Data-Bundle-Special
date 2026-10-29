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
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.web.client.HttpStatusCodeException;

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
        List<String> refs = recipients.stream().map(r -> UUID.randomUUID().toString()).toList();
        return sendBulkSms(senderId, message, recipients, refs);
    }

    public List<String> sendBulkSms(String senderId, String message, List<String> recipients, List<String> refs) {
        if (recipients == null || refs == null || recipients.size() != refs.size())
            throw new IllegalArgumentException("Recipient and reference counts must match");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-VASKEY", apiKey);
        headers.set("Content-Type", "application/json");

        List<MoolreSmsMessage> messages = new ArrayList<>();
        for (int i = 0; i < recipients.size(); i++) {
            String recipient = recipients.get(i);
            String ref = refs.get(i);
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
                throw new MoolreSmsException(body != null ? body.getCode() : null,
                        body != null ? body.getMessage() : "No response", true);
            }
        } catch (HttpStatusCodeException e) {
            boolean definitive = e.getStatusCode().is4xxClientError();
            throw new MoolreSmsException("HTTP_" + e.getStatusCode().value(),
                    "Moolre HTTP " + e.getStatusCode().value(), definitive, e);
        } catch (Exception e) {
            if (e instanceof MoolreSmsException smsException) throw smsException;
            log.error("Exception dispatching SMS to Moolre", e);
            throw new MoolreSmsException(null, "Moolre outcome is unknown", false, e);
        }
    }

    /** Moolre's SMS status endpoint returns raw numeric delivery states; retain them without guessing their meaning. */
    public Map<String, Integer> checkMessageStatuses(List<String> references) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-VASKEY", apiKey);
        headers.set("Content-Type", "application/json");
        Map<String, Object> payload = Map.of("type", 5, "ref", references);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl.replace("/send", "/status"),
                    HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
            Map body = response.getBody();
            if (body == null || !"1".equals(String.valueOf(body.get("status"))))
                throw new IllegalStateException("Moolre did not return SMS status data");
            Object data = body.get("data");
            Map<String, Integer> result = new LinkedHashMap<>();
            if (data instanceof List<?> items) for (Object item : items) {
                if (item instanceof Map<?, ?> entry && entry.get("ref") != null && entry.get("status") instanceof Number state)
                    result.put(String.valueOf(entry.get("ref")), state.intValue());
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reconcile SMS delivery status", e);
        }
    }

    public static class MoolreSmsException extends RuntimeException {
        private final String providerCode;
        private final boolean definitiveRejection;
        public MoolreSmsException(String providerCode, String message, boolean definitiveRejection) {
            super(message); this.providerCode = providerCode; this.definitiveRejection = definitiveRejection;
        }
        public MoolreSmsException(String providerCode, String message, boolean definitiveRejection, Throwable cause) {
            super(message, cause); this.providerCode = providerCode; this.definitiveRejection = definitiveRejection;
        }
        public String getProviderCode() { return providerCode; }
        public boolean isDefinitiveRejection() { return definitiveRejection; }
    }

    public static int calculatePages(String message) {
        if (message == null || message.isEmpty()) return 0;
        String gsmBasic = "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";
        String gsmExtension = "^{}\\[~]|€";
        boolean gsm = message.codePoints().allMatch(cp -> gsmBasic.indexOf(cp) >= 0 || gsmExtension.indexOf(cp) >= 0);
        if (!gsm) {
            int units = message.length(); // Java length counts UTF-16 units, as UCS-2 segment accounting requires.
            return units <= 70 ? 1 : (int) Math.ceil(units / 67.0);
        }
        int units = message.codePoints().map(cp -> gsmExtension.indexOf(cp) >= 0 ? 2 : 1).sum();
        return units <= 160 ? 1 : (int) Math.ceil(units / 153.0);
    }
}
