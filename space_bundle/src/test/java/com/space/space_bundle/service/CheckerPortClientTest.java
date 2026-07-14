package com.space.space_bundle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.space_bundle.dto.checkerport.CheckerPortVoucherRequest;
import com.space.space_bundle.dto.checkerport.CheckerPortResponse;
import com.space.space_bundle.exception.CheckerPortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckerPortClientTest {

    private CheckerPortClient client;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        client = new CheckerPortClient(webClientBuilder, new ObjectMapper(), "http://localhost", "apikey");
    }

    @Test
    void buyVoucher_Success() {
        CheckerPortResponse<Map<String, Object>> mockResp = new CheckerPortResponse<>();
        mockResp.setStatus("SUCCESS");
        
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(mockResp));

        CheckerPortVoucherRequest req = new CheckerPortVoucherRequest("PlatformWaecNew", 1, "0240000000", BigDecimal.valueOf(20), "url", null);
        CheckerPortResponse<Map<String, Object>> response = client.buyVoucher(req);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(req.getReferenceId()); // Verify referenceId was generated
    }

    @Test
    void buyVoucher_ThrowsExceptionOnError() {
        CheckerPortResponse<Map<String, Object>> mockResp = new CheckerPortResponse<>();
        mockResp.setStatus("FAILED");
        mockResp.setErrorCode("INPUT_INVALID");
        mockResp.setMessage("Invalid input");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(mockResp));

        CheckerPortVoucherRequest req = new CheckerPortVoucherRequest();
        
        CheckerPortException exception = assertThrows(CheckerPortException.class, () -> {
            client.buyVoucher(req);
        });

        assertEquals("INPUT_INVALID", exception.getErrorCode());
        assertEquals("Invalid input", exception.getMessage());
    }
}
