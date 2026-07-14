package com.space.space_bundle.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "results_transactions")
public class ResultsTransaction {

    @Id
    private String id;

    @Indexed(unique = true)
    private String referenceId;

    @Indexed
    private String userId; // Optional, if logged in

    private ResultsTransactionType type; // VOUCHER, ARC_BECE, etc.

    @Indexed
    private ServiceStatus status; // PENDING, PENDING_INPUT, COMPLETE, FAILED

    private String phoneNumber;
    private BigDecimal price; // Price charged to the user
    private BigDecimal costPrice; // Cost price from CheckerPort
    private Integer qty; // For vouchers
    private String platform; // PlatformWaecNew, PlatformWaecOld

    // Specific to ARC
    private String indexNumber;
    private String year;
    private String dob;

    // Response Data
    private String message;
    private String errorCode;
    private String statusCode; // Used when pending-input to determine the issue

    // Store raw result payload or vouchers
    private Map<String, Object> resultData;
    private Object vouchers; // List of vouchers

    // Webhook tracking
    private boolean webhookReceived;
    private int pollAttempts; // for the polling job

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
