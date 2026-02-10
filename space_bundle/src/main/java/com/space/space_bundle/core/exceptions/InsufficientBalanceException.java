package com.space.space_bundle.core.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    
    private final BigDecimal currentBalance;
    private final BigDecimal requiredAmount;
    
    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("Insufficient balance. Current: GHS %.2f, Required: GHS %.2f", 
              currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }
}