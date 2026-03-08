package com.space.space_bundle.in.web.dto;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.entities.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardStats {
    private BigDecimal totalSpent;
    private long thisMonthOrders;
    private double spendingIncreasePercentage;
    private List<Transaction> recentTransactions;
    private List<Bundle> popularBundles;
}
