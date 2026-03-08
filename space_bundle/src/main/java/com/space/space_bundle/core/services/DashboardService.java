package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.Bundle;
import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.entities.Transaction;
import com.space.space_bundle.in.web.dto.DashboardStats;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DashboardService {
    private final TransactionService transactionService;
    private final OrderService orderService;
    private final BundleService bundleService;

    public DashboardStats getStats(String userId) {
        List<Transaction> allTransactions = transactionService.getUserTransactions(userId);
        List<Order> allOrders = orderService.getOrders(userId, null, null, null);
        
        // Calculate total spent (Completed DEBIT transactions)
        BigDecimal totalSpent = allTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Orders this month
        LocalDateTime beginningOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long thisMonthOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(beginningOfMonth))
                .count();

        // Recent transactions (last 5)
        List<Transaction> recentTransactions = allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Popular bundles (for now just take top 4 available bundles)
        List<Bundle> allBundles = bundleService.getAllBundles();
        List<Bundle> popularBundles = allBundles.stream()
                .limit(4)
                .collect(Collectors.toList());

        // Spending increase (mocked for now or can be calculated)
        double spendingIncreasePercentage = 25.0; // Mocked

        return DashboardStats.builder()
                .totalSpent(totalSpent)
                .thisMonthOrders(thisMonthOrders)
                .spendingIncreasePercentage(spendingIncreasePercentage)
                .recentTransactions(recentTransactions)
                .popularBundles(popularBundles)
                .build();
    }
}
