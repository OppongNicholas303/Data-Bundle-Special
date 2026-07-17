package com.space.space_bundle.controller;

import com.space.space_bundle.service.ResultsCheckerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckerPortWebhookControllerTest {

    @Mock
    private ResultsCheckerService resultsCheckerService;

    @InjectMocks
    private CheckerPortWebhookController webhookController;

    private final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookController, "configuredApiKey", API_KEY);
    }

    @Test
    void handleWebhook_Unauthorized_Returns401() {
        Map<String, Object> payload = new HashMap<>();
        ResponseEntity<Void> response = webhookController.handleWebhook("wrong-key", payload);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(resultsCheckerService, never()).handleWebhook(any());
    }

    @Test
    void handleWebhook_Valid_CallsService() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "SUCCESS");

        ResponseEntity<Void> response = webhookController.handleWebhook(API_KEY, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(resultsCheckerService).handleWebhook(payload);
    }

    @Test
    void handleWebhook_ServiceThrowsException_Returns500() {
        Map<String, Object> payload = new HashMap<>();
        doThrow(new RuntimeException("Test Exception")).when(resultsCheckerService).handleWebhook(payload);

        ResponseEntity<Void> response = webhookController.handleWebhook(API_KEY, payload);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(resultsCheckerService).handleWebhook(payload);
    }
}
