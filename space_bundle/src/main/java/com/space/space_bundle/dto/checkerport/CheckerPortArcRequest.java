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
public class CheckerPortArcRequest {
    private String type; // Bece, WassceSchool, WasscePrivate, ShsPlacement
    private String indexNumber;
    private String phoneNumber;
    private String email;
    private BigDecimal price;
    private String webhookCallbackUrl;
    private String referenceId;
    private String year;
    private String dob;
    private String agentCode;
}
