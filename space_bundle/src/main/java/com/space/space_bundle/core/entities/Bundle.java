package com.space.space_bundle.core.entities;

import java.math.BigDecimal;

public class Bundle {

    private final String code;
    private final String dataSize;
    private final BigDecimal costPrice;
    private final BigDecimal sellingPrice;

    public Bundle(String code, String dataSize, BigDecimal costPrice, BigDecimal sellingPrice) {
        this.code = code;
        this.dataSize = dataSize;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
    }

    // Getters
    public String getCode() { return code; }
    public String getDataSize() { return dataSize; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
}

