package com.space.space_bundle.core.port.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PackageDto(
        Long id,
        String name,
        String size,
        String network,

        @JsonProperty("validity_days")
        Integer validityDays,

        @JsonProperty("cost_price")
        String costPrice
) {}