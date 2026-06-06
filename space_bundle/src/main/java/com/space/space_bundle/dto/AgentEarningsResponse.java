package com.space.space_bundle.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AgentEarningsResponse {
    private BigDecimal totalSales;
    private BigDecimal totalProfit;
    private BigDecimal walletBalance;
    private List<?> commissions;
}


