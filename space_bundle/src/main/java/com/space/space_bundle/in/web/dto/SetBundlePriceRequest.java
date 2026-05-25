package com.space.space_bundle.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SetBundlePriceRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;
}
