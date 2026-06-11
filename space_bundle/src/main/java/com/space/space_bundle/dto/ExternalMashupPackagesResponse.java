package com.space.space_bundle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMashupPackagesResponse {
    private boolean success;
    private List<ExternalMashupPackage> packages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalMashupPackage {
        private Integer id;

        @JsonProperty("special_offer_package_id")
        private Integer specialOfferPackageId;

        private String slug;
        private String name;
        private String description;

        @JsonProperty("data_amount_mb")
        private BigDecimal dataAmountMb;

        private String network;

        @JsonProperty("cost_price")
        private BigDecimal costPrice;
    }
}
