package com.space.space_bundle.core.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Transaction {
    private String id;
    private String userId;
    private String orderId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String currency;
    private TransactionStatus status;
    private String reference;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TransactionType {
        DEBIT, CREDIT, REFUND
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = TransactionStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
}
