package com.space.space_bundle.core.entities;

import com.space.space_bundle.core.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Order {

    private final String id;
    private final String userId;

    private final String network;       // MTN, VODAFONE, AIRTELTIGO
    private final String phoneNumber;
    private final String bundleCode;

    private final BigDecimal amount;

    private OrderStatus status;
    private String providerReference;
    private String failureReason;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Order(
            String id,
            String userId,
            String network,
            String phoneNumber,
            String bundleCode,
            BigDecimal amount,
            OrderStatus status,
            String providerReference,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.userId = userId;
        this.network = network;
        this.phoneNumber = phoneNumber;
        this.bundleCode = bundleCode;
        this.amount = amount;

        this.status = status == null ? OrderStatus.CREATED : status;
        this.providerReference = providerReference;
        this.failureReason = failureReason;

        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt;
    }

    /* =====================
       BUSINESS OPERATIONS
       ===================== */

    public void markPaid() {
        assertStatus(OrderStatus.CREATED);
        this.status = OrderStatus.PAID;
        touch();
    }

    public void markProcessing() {
        assertStatus(OrderStatus.PAID);
        this.status = OrderStatus.PROCESSING;
        touch();
    }

    public void markCompleted(String providerReference) {
        assertStatus(OrderStatus.PROCESSING);
        this.status = OrderStatus.COMPLETED;
        this.providerReference = providerReference;
        touch();
    }

    public void markFailed(String reason) {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Completed order cannot fail");
        }
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
        touch();
    }

    public void markRefunded() {
        assertStatus(OrderStatus.FAILED);
        this.status = OrderStatus.REFUNDED;
        touch();
    }

    /* =====================
       VALIDATIONS
       ===================== */

    private void assertStatus(OrderStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Invalid order state transition: " + this.status + " → " + expected
            );
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
