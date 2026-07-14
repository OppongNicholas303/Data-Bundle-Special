package com.space.space_bundle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.space_bundle.dto.checkerport.*;
import com.space.space_bundle.exception.CheckerPortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class CheckerPortClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${checkerport.webhook-callback-url}")
    private String webhookCallbackUrl;

    public CheckerPortClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${checkerport.base-url:https://api.checkerport.com}") String baseUrl,
            @Value("${checkerport.api-key}") String apiKey) {
        
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", apiKey)
                .build();
    }

    public String generateReferenceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase();
    }

    public CheckerPortResponse<Map<String, Object>> buyVoucher(CheckerPortVoucherRequest request) {
        if (request.getWebhookCallbackUrl() == null) request.setWebhookCallbackUrl(webhookCallbackUrl);
        if (request.getReferenceId() == null) request.setReferenceId(generateReferenceId());
        return executePost("/voucher-sale/create", request, new ParameterizedTypeReference<CheckerPortResponse<Map<String, Object>>>() {});
    }

    public CheckerPortResponse<Map<String, Object>> checkResult(CheckerPortArcRequest request) {
        if (request.getWebhookCallbackUrl() == null) request.setWebhookCallbackUrl(webhookCallbackUrl);
        if (request.getReferenceId() == null) request.setReferenceId(generateReferenceId());
        return executePost("/arc/create", request, new ParameterizedTypeReference<CheckerPortResponse<Map<String, Object>>>() {});
    }

    public CheckerPortResponse<Map<String, Object>> submitCorrection(CheckerPortCorrectionRequest request) {
        if (request.getWebhookCallbackUrl() == null) request.setWebhookCallbackUrl(webhookCallbackUrl);
        return executePost("/arc/correction", request, new ParameterizedTypeReference<CheckerPortResponse<Map<String, Object>>>() {});
    }

    public CheckerPortResponse<Map<String, Object>> getStatus(String referenceId) {
        return executeGet("/status-check/" + referenceId, new ParameterizedTypeReference<CheckerPortResponse<Map<String, Object>>>() {});
    }

    public CheckerPortResponse<Map<String, Object>> getServicePrice(String serviceName) {
        return executeGet("/service-price?serviceName=" + serviceName, new ParameterizedTypeReference<CheckerPortResponse<Map<String, Object>>>() {});
    }

    private <T> T executePost(String uri, Object body, ParameterizedTypeReference<T> responseType) {
        try {
            T response = webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2)).filter(this::isConnectionError))
                    .block();
            checkResponseForError(response);
            return response;
        } catch (WebClientResponseException e) {
            return handleHttpError(e);
        }
    }

    private <T> T executeGet(String uri, ParameterizedTypeReference<T> responseType) {
        try {
            T response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2)).filter(this::isConnectionError))
                    .block();
            checkResponseForError(response);
            return response;
        } catch (WebClientResponseException e) {
            return handleHttpError(e);
        }
    }

    private boolean isConnectionError(Throwable throwable) {
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException ||
               (throwable.getMessage() != null && throwable.getMessage().contains("Connection refused"));
    }

    private <T> void checkResponseForError(T response) {
        if (response instanceof CheckerPortResponse<?> checkerResponse) {
            if ("FAILED".equalsIgnoreCase(checkerResponse.getStatus())) {
                throw new CheckerPortException(checkerResponse.getMessage(), checkerResponse.getErrorCode());
            }
        }
    }

    private <T> T handleHttpError(WebClientResponseException e) {
        String body = e.getResponseBodyAsString();
        try {
            CheckerPortResponse<?> errorResp = objectMapper.readValue(body, CheckerPortResponse.class);
            if (errorResp != null && errorResp.getErrorCode() != null) {
                throw new CheckerPortException(errorResp.getMessage(), errorResp.getErrorCode());
            }
        } catch (JsonProcessingException ignored) { }
        
        throw new CheckerPortException("HTTP Request failed with status " + e.getStatusCode() + ": " + body, String.valueOf(e.getStatusCode().value()));
    }
}
