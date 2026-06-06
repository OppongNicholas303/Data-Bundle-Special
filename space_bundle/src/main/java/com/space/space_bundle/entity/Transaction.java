package com.space.space_bundle.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String orderId;

    private String type;   // DEBIT, CREDIT, REFUND, COMMISSION, WITHDRAWAL, PURCHASE, FUNDING, SETTLEMENT
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String currency;
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED
    private String reference;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Type {
        DEBIT, CREDIT, REFUND, COMMISSION, WITHDRAWAL, PURCHASE, FUNDING, SETTLEMENT
    }

    public enum Status {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
}
