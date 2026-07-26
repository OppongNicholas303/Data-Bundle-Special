package com.space.space_bundle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MashupBundleRequest {

    @NotNull(message = "External ID is required")
    @Positive(message = "External ID must be a positive number")
    private Integer externalId;

    @NotNull(message = "Special offer package ID is required")
    @Positive(message = "Special offer package ID must be a positive number")
    private Integer specialOfferPackageId;

    @NotBlank(message = "Slug is required")
    private String slug;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Data amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Data amount must be greater than zero")
    private BigDecimal dataAmountMb;

    @NotBlank(message = "Data size is required")
    private String dataSize;

    @NotBlank(message = "Network is required")
    private String network;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than zero")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;
}
