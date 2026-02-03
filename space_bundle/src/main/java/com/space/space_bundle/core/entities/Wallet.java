package com.space.space_bundle.core.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Wallet {
    private String id;
    private String userId;
    private BigDecimal balance;
    private String currency;
    private WalletStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum WalletStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        balance = balance.subtract(amount);
        updatedAt = LocalDateTime.now();
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
        updatedAt = LocalDateTime.now();
    }
}
