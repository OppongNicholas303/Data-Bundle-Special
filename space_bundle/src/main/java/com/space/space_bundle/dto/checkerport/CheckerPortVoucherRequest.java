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
    private BigDecimal price;
    private String webhookCallbackUrl;
    private String referenceId;
}
