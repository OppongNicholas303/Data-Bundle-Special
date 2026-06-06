package com.space.space_bundle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotBlank(message = "Network is required")
    @Pattern(
        regexp = "(?i)^(mtn|telecel|airteltigo|vodafone)$",
        message = "Invalid network"
    )
    private String network;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(0|\\+233)[0-9]{9}$", message = "Invalid Ghana phone number")
    private String phoneNumber;

    @NotBlank(message = "Bundle code is required")
    @Size(max = 10, message = "Invalid bundle code")
    private String bundleCode;

    private String email;
    private String package_id;

    @Size(max = 20, message = "Invalid agent code")
    private String agentCode;
}
