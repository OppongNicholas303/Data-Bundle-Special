package com.space.space_bundle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionView {
    private String id;
    private String userId;
    private String orderId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String currency;
    private String status;
    private String reference;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User Info
    private String userEmail;
    private String userPhone;
    private String username;
}
