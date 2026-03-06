package com.space.space_bundle.core.entities;

import com.space.space_bundle.core.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Order {

    private final String id;
    private final String userId;

    private final String network;       // MTN, VODAFONE, AIRTELTIGO
    private final String phoneNumber;
    private final String bundleCode;
    private final int package_id;

    private final BigDecimal amount;

    private OrderStatus status;
    private String providerStatus;
    private String providerOrderNumber;
    private String providerReference;
    private String paymentReference; // Paystack reference
    private String paymentUrl; // Paystack authorization URL
    private String paymentAccessCode; // Paystack access code
    private String failureReason;
    private String byFrom;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Order(
            String id,
            String userId,
            String network,
            String phoneNumber,
            String bundleCode,
            int packageId,
            BigDecimal amount,
            OrderStatus status,
            String providerStatus,
            String providerOrderNumber,
            String providerReference,
            String paymentReference,
            String paymentUrl,
            String byFrom,
            String paymentAccessCode,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.userId = userId;
        this.network = network;
        this.phoneNumber = phoneNumber;
        this.bundleCode = bundleCode;
        this.package_id = packageId;
        this.amount = amount;

        this.providerStatus = providerStatus;

        this.status = status == null ? OrderStatus.CREATED : status;
        this.providerOrderNumber = providerOrderNumber;

        this.paymentReference = paymentReference;
        this.providerReference = providerReference;
        this.paymentUrl = paymentUrl;
        this.byFrom = byFrom;
        this.paymentAccessCode = paymentAccessCode;
        this.failureReason = failureReason;

        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt;
    }

    /* =====================
       BUSINESS OPERATIONS
       ===================== */

    public void setPendingPayment(String paymentReference, String paymentUrl, String paymentAccessCode) {
        assertStatus(OrderStatus.CREATED);
        this.status = OrderStatus.PENDING_PAYMENT;
        this.paymentReference = paymentReference;
        this.paymentUrl = paymentUrl;
        this.paymentAccessCode = paymentAccessCode;
        touch();
    }

    public void markPaid() {
        if (this.status != OrderStatus.CREATED && this.status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Invalid order state for payment: " + this.status);
        }
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
        this.providerOrderNumber = providerReference;
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