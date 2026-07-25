package com.space.space_bundle.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AgentWithdrawalRequest {

    @NotNull
    @DecimalMin(value = "10.00", message = "Minimum withdrawal is GHS 10.00")
    @DecimalMax(value = "10000.00", message = "Maximum single withdrawal is GHS 10,000")
    private BigDecimal amount;

    /**
     * MoMo provider: MTN, VODAFONE (Telecel), AIRTELTIGO
     * Maps to provider mobile_money subtype
     */
    @NotBlank(message = "MoMo provider is required")
    @Pattern(regexp = "(?i)^(mtn|vodafone|telecel|airteltigo)$", message = "Invalid MoMo provider")
    private String momoProvider;

    /**
     * Ghana mobile number that receives the MoMo transfer
     */
    @NotBlank(message = "MoMo number is required")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Invalid Ghana phone number")
    private String momoNumber;

    /**
     * Account name as registered on the MoMo wallet
     */
    @NotBlank(message = "Account name is required")
    @Size(min = 2, max = 100, message = "Account name must be 2-100 characters")
    @Pattern(regexp = "^[a-zA-Z \\-']+$", message = "Account name contains invalid characters")
    private String accountName;
}
