package com.space.space_bundle.service;

import com.space.space_bundle.dto.checkerport.CheckerPortResponse;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.entity.ServiceStatus;
import com.space.space_bundle.repository.ResultsTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckerPortPollingJobTest {

    @Mock
    private ResultsTransactionRepository transactionRepository;

    @Mock
    private CheckerPortClient checkerPortClient;

    @InjectMocks
    private CheckerPortPollingJob pollingJob;

    @Test
    void pollStaleTransactions_UpdatesStaleTransaction() {
        ResultsTransaction tx = new ResultsTransaction();
        tx.setReferenceId("REF123");
        tx.setStatus(ServiceStatus.PENDING);
        tx.setPollAttempts(0);

        when(transactionRepository.findByStatusInAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of(tx));

        CheckerPortResponse<Map<String, Object>> mockResponse = new CheckerPortResponse<>();
        mockResponse.setStatus("SUCCESS");
        Map<String, Object> data = new HashMap<>();
        data.put("serviceStatus", "complete");
        mockResponse.setData(data);

        when(checkerPortClient.getStatus("REF123")).thenReturn(mockResponse);

        pollingJob.pollStaleTransactions();

        assertEquals(ServiceStatus.COMPLETE, tx.getStatus());
        assertEquals(1, tx.getPollAttempts());
        verify(transactionRepository).save(tx);
    }

    @Test
    void pollStaleTransactions_SkipsMaxAttempts() {
        ResultsTransaction tx = new ResultsTransaction();
        tx.setReferenceId("REF123");
        tx.setStatus(ServiceStatus.PENDING);
        tx.setPollAttempts(10); // Max reached

        when(transactionRepository.findByStatusInAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of(tx));

        pollingJob.pollStaleTransactions();

        verify(checkerPortClient, never()).getStatus(any());
        verify(transactionRepository, never()).save(any());
    }
}
