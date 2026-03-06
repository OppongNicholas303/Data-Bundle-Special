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
@Document(collection = "orders")
public class OrderDocument {
    @Id
    private String id;
    private String userId;
    private String network;
    private String phoneNumber;
    private String bundleCode;
    private String package_id;
    private BigDecimal amount;
    private String status;
    private String providerStatus;
    private String providerOrderNumber;
    private String providerReference;
    private String paymentReference;
    private String paymentUrl;
    private String paymentAccessCode;
    private String failureReason;
    private String byFrom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}