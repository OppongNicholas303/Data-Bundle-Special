package com.space.space_bundle.core.entities;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AgentProfile {
    private String id;
    private String userId;
    private String businessName;
    private String referralCode;   // unique short code e.g. "AGENT-X7K2" — used in storefront links
    private BigDecimal totalSales;
    private BigDecimal totalProfit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void recordSale(BigDecimal saleAmount, BigDecimal profit) {
        this.totalSales = this.totalSales.add(saleAmount);
        this.totalProfit = this.totalProfit.add(profit);
        this.updatedAt = LocalDateTime.now();
    }
}
