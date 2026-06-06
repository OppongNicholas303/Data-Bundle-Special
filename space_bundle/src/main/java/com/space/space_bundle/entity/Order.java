package com.space.space_bundle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String agentId;          // agentProfile.id — null for direct orders

    private String network;
    private String phoneNumber;
    private String bundleCode;
    private String packageId;

    private BigDecimal amount;        // total customer pays (includes Paystack fee)

    @JsonIgnore  // Internal — never expose platform base price or agent margin to customers
    private BigDecimal baseAmount;     // bundle sellingPrice (platform charge before fee)

    @JsonIgnore
    private BigDecimal costPrice;      // bundle costPrice (what TapData pays the provider)

    @JsonIgnore
    private BigDecimal commissionAmount;

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

    public enum OrderStatus {
        CREATED, PENDING_PAYMENT, PAID, PROCESSING, COMPLETED, FAILED, REFUNDED
    }

    // ── State transitions ──────────────────────────────────────────────────

    public void setPendingPayment(String paymentReference, String paymentUrl, String paymentAccessCode) {
        assertStatus(OrderStatus.CREATED);
        this.status = OrderStatus.PENDING_PAYMENT.name();
        this.paymentReference = paymentReference;
        this.paymentUrl = paymentUrl;
        this.paymentAccessCode = paymentAccessCode;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaid() {
        if (!OrderStatus.CREATED.name().equals(status) && !OrderStatus.PENDING_PAYMENT.name().equals(status))
            throw new IllegalStateException("Invalid state for payment: " + status);
        this.status = OrderStatus.PAID.name();
        this.updatedAt = LocalDateTime.now();
    }

    public void markProcessing() {
        assertStatus(OrderStatus.PAID);
        this.status = OrderStatus.PROCESSING.name();
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted(String providerReference) {
        assertStatus(OrderStatus.PROCESSING);
        this.status = OrderStatus.COMPLETED.name();
        this.providerOrderNumber = providerReference;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        if (OrderStatus.COMPLETED.name().equals(status))
            throw new IllegalStateException("Completed order cannot fail");
        this.status = OrderStatus.FAILED.name();
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void markRefunded() {
        assertStatus(OrderStatus.FAILED);
        this.status = OrderStatus.REFUNDED.name();
        this.updatedAt = LocalDateTime.now();
    }

    private void assertStatus(OrderStatus expected) {
        if (!expected.name().equals(this.status))
            throw new IllegalStateException("Expected " + expected + " but was " + status);
    }
}
