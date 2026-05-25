package com.space.space_bundle.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentWithdrawalRequest {
    @NotNull
    @DecimalMin(value = "10.00", message = "Minimum withdrawal is GHS 10.00")
    private BigDecimal amount;

    @NotBlank
    private String bankCode;

    @NotBlank
    private String accountNumber;

    @NotBlank
    private String accountName;
}
