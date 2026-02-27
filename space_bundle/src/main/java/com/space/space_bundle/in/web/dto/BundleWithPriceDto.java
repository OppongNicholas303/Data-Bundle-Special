package com.space.space_bundle.in.web.dto;

import java.math.BigDecimal;

public record BundleWithPriceDto(
        Long id,
        String name,
        String size,
        String network,
        Integer validityDays,
        String costPrice,
        BigDecimal sellingPrice
) {}