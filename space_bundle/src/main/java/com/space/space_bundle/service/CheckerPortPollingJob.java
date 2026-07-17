package com.space.space_bundle.service;

import com.space.space_bundle.dto.checkerport.CheckerPortResponse;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.entity.ServiceStatus;
import com.space.space_bundle.repository.ResultsTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckerPortPollingJob {

    private final ResultsTransactionRepository transactionRepository;
    private final CheckerPortClient checkerPortClient;

    // Run every 2 minutes
    @Scheduled(fixedDelay = 120000)
    public void pollStaleTransactions() {
        LocalDateTime sevenMinsAgo = LocalDateTime.now().minusMinutes(7);
        List<ResultsTransaction> staleTransactions = transactionRepository.findByStatusInAndCreatedAtBefore(
                List.of(ServiceStatus.PENDING, ServiceStatus.PENDING_INPUT), sevenMinsAgo
        );

        for (ResultsTransaction tx : staleTransactions) {
            if (tx.getPollAttempts() >= 10) {
                log.warn("Max poll attempts reached for tx {}. Marking as FAILED.", tx.getReferenceId());
                tx.setStatus(ServiceStatus.FAILED);
                tx.setMessage("Transaction timed out after max poll attempts.");
                tx.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                continue;
            }

            try {
                CheckerPortResponse<Map<String, Object>> resp = checkerPortClient.getStatus(tx.getReferenceId());
                tx.setPollAttempts(tx.getPollAttempts() + 1);

                ServiceStatus newStatus = mapServiceStatus(resp.getData().get("serviceStatus"));
                if (newStatus == null) {
                    newStatus = "FAILED".equalsIgnoreCase(resp.getStatus()) ? ServiceStatus.FAILED : tx.getStatus();
                }

                if (newStatus != tx.getStatus()) {
                    tx.setStatus(newStatus);
                    tx.setMessage(resp.getMessage());
                    tx.setErrorCode(resp.getErrorCode());

                    if (resp.getData().containsKey("statusCode")) {
                        tx.setStatusCode((String) resp.getData().get("statusCode"));
                    }
                    if (resp.getData().containsKey("result")) {
                        tx.setResultData((Map<String, Object>) resp.getData().get("result"));
                    }
                    if (resp.getData().containsKey("vouchers")) {
                        tx.setVouchers(resp.getData().get("vouchers"));
                    }
                }

                tx.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(tx);

            } catch (Exception e) {
                log.error("Error polling tx {}: {}", tx.getReferenceId(), e.getMessage());
                tx.setPollAttempts(tx.getPollAttempts() + 1);
                transactionRepository.save(tx);
            }
        }
    }

    private ServiceStatus mapServiceStatus(Object statusObj) {
        if (statusObj == null) return null;
        String statusStr = statusObj.toString();
        switch (statusStr.toLowerCase()) {
            case "pending": return ServiceStatus.PENDING;
            case "pending-input": return ServiceStatus.PENDING_INPUT;
            case "complete": return ServiceStatus.COMPLETE;
            default: return null;
        }
    }
}
