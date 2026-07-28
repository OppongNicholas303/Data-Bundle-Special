package com.space.space_bundle.dto.checkerport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckerPortVoucherRequest {
    private String platform; // PlatformWaecNew, PlatformWaecOld
    private Integer qty;
    private String phoneNumber;
    private String email;
    private BigDecimal price;
    private BigDecimal amount;
    private String webhookCallbackUrl;
    private String referenceId;
    private String agentCode;
}
