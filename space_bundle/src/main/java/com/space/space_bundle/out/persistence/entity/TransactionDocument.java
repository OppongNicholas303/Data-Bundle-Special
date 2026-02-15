package com.space.space_bundle.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class TransactionDocument {
    @Id
    private String id;
    private String userId;
    private String orderId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String reference;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}