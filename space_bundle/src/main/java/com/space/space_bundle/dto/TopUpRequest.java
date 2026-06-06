package com.space.space_bundle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TopUpRequest {
    @NotNull @DecimalMin("1.00") private BigDecimal amount;
}
