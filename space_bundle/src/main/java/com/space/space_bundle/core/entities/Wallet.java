package com.space.space_bundle.core.entities;

import java.math.BigDecimal;
import java.util.UUID;

public class Wallet {

    private final UUID userId;
    private BigDecimal balance;

    public Wallet(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    // ---------------------
    // Business rules
    // ---------------------
    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    // ---------------------
    // Getters
    // ---------------------
    public UUID getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
}
