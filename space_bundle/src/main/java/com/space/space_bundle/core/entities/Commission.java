package com.space.space_bundle.core.entities;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable commission record created when an agent earns profit from a sale.
 * Never mutate fields after creation — use status transitions only.
 */
@Getter
@Builder
public class Commission {
    private final String id;
    private final String agentId;
    private final String orderId;
    private final BigDecimal baseAmount;
    private final BigDecimal sellingAmount;
    private final BigDecimal profit;
    private CommissionStatus status;
    private final LocalDateTime createdAt;

    public enum CommissionStatus {
        PENDING, SETTLED, REVERSED
    }

    public void settle() {
        if (this.status != CommissionStatus.PENDING) {
            throw new IllegalStateException("Commission already " + this.status);
        }
        this.status = CommissionStatus.SETTLED;
    }

    public void reverse() {
        if (this.status == CommissionStatus.REVERSED) {
            throw new IllegalStateException("Commission already reversed");
        }
        this.status = CommissionStatus.REVERSED;
    }
}
