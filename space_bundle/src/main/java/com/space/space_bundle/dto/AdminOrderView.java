package com.space.space_bundle.dto;

import com.space.space_bundle.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminOrderView {

    private String id;
    private String userId;
    private String agentId;
    private String network;
    private String phoneNumber;
    private String bundleCode;
    private String bundleType;
    private String packageId;

    /** Total amount the customer paid (sellingPrice + 2% Moolre fee) */
    private BigDecimal amount;

    /** Bundle selling price — what TapData charges before the Moolre fee */
    private BigDecimal baseAmount;

    /** Bundle cost price — what TapData pays the provider */
    private BigDecimal costPrice;

    /** Commission the agent earns on this order (0 for direct orders) */
    private BigDecimal commissionAmount;

    /**
     * TapData platform profit:
     *   sellingPrice (baseAmount) - costPrice - agentCommission
     * The Moolre fee is excluded — that goes entirely to Moolre.
     */
    private BigDecimal platformProfit;

    private String status;
    private String providerStatus;
    private String failureReason;
    private String byFrom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminOrderView from(Order o) {
        BigDecimal selling    = o.getBaseAmount()       != null ? o.getBaseAmount()       : BigDecimal.ZERO;
        BigDecimal cost       = o.getCostPrice()        != null ? o.getCostPrice()        : BigDecimal.ZERO;
        BigDecimal commission = o.getCommissionAmount() != null ? o.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal amount     = o.getAmount()           != null ? o.getAmount()           : BigDecimal.ZERO;

        // profit = selling price - provider cost - agent commission
        BigDecimal profit = selling.subtract(cost).subtract(commission);

        return AdminOrderView.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .agentId(o.getAgentId())
                .network(o.getNetwork())
                .phoneNumber(o.getPhoneNumber())
                .bundleCode(o.getBundleCode())
                .bundleType(o.getBundleType())
                .packageId(o.getPackageId())
                .amount(amount)
                .baseAmount(selling)
                .costPrice(cost)
                .commissionAmount(commission)
                .platformProfit(profit)
                .status(o.getStatus())
                .providerStatus(o.getProviderStatus())
                .failureReason(o.getFailureReason())
                .byFrom(o.getByFrom())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
