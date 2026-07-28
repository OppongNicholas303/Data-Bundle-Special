package com.space.space_bundle.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceResponse {
    private BigDecimal balance;
    private BigDecimal commissionBalance;
    private String currency;
}
