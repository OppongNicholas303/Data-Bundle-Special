package com.space.space_bundle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.space_bundle.dto.checkerport.CheckerPortResponse;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.entity.ServiceStatus;
import com.space.space_bundle.repository.ResultsTransactionRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckerPortWebhookControllerTest {

    @Mock
    private ResultsTransactionRepository transactionRepository;

    @InjectMocks
    private CheckerPortWebhookController webhookController;

    private final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookController, "configuredApiKey", API_KEY);
    }

    @Test
    void handleWebhook_Unauthorized_Returns401() {
        CheckerPortResponse<Map<String, Object>> payload = new CheckerPortResponse<>();
        ResponseEntity<Void> response = webhookController.handleWebhook("wrong-key", payload);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleWebhook_Valid_UpdatesTransaction() {
        String refId = "REF123";
        CheckerPortResponse<Map<String, Object>> payload = new CheckerPortResponse<>();
        payload.setStatus("SUCCESS");
        Map<String, Object> data = new HashMap<>();
        data.put("referenceId", refId);
        data.put("serviceStatus", "complete");
        payload.setData(data);

        ResultsTransaction tx = new ResultsTransaction();
        tx.setReferenceId(refId);
        tx.setStatus(ServiceStatus.PENDING);

        when(transactionRepository.findByReferenceId(refId)).thenReturn(Optional.of(tx));

        ResponseEntity<Void> response = webhookController.handleWebhook(API_KEY, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ServiceStatus.COMPLETE, tx.getStatus());
        verify(transactionRepository).save(tx);
    }

    @Test
    void handleWebhook_Idempotent_DoesNotUpdateIfAlreadyComplete() {
        String refId = "REF123";
        CheckerPortResponse<Map<String, Object>> payload = new CheckerPortResponse<>();
        Map<String, Object> data = new HashMap<>();
        data.put("referenceId", refId);
        data.put("serviceStatus", "complete");
        payload.setData(data);

        ResultsTransaction tx = new ResultsTransaction();
        tx.setReferenceId(refId);
        tx.setStatus(ServiceStatus.COMPLETE);
        tx.setWebhookReceived(true); // Already received

        when(transactionRepository.findByReferenceId(refId)).thenReturn(Optional.of(tx));

        ResponseEntity<Void> response = webhookController.handleWebhook(API_KEY, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(transactionRepository, never()).save(any());
    }
}
