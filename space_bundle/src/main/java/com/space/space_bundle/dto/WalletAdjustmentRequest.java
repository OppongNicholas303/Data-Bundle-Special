package com.space.space_bundle.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletAdjustmentRequest {
    private BigDecimal amount;
    private String description;
}
