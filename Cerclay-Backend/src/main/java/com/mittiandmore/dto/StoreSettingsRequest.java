package com.mittiandmore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class StoreSettingsRequest {

    @NotNull(message = "GST rate is required")
    @DecimalMin(
            value = "0.00",
            message = "GST rate cannot be negative"
    )
    private BigDecimal gstRate;

    @NotNull(message = "Shipping charge is required")
    @DecimalMin(
            value = "0.00",
            message = "Shipping charge cannot be negative"
    )
    private BigDecimal shippingCharge;

    @NotNull(message = "Free shipping threshold is required")
    @DecimalMin(
            value = "0.00",
            message = "Free shipping threshold cannot be negative"
    )
    private BigDecimal freeShippingThreshold;

    @NotNull(message = "Minimum order value is required")
    @DecimalMin(
            value = "0.00",
            message = "Minimum order value cannot be negative"
    )
    private BigDecimal minimumOrderValue;

    private String whatsappNumber;

    public StoreSettingsRequest() {
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public BigDecimal getShippingCharge() {
        return shippingCharge;
    }

    public void setShippingCharge(BigDecimal shippingCharge) {
        this.shippingCharge = shippingCharge;
    }

    public BigDecimal getFreeShippingThreshold() {
        return freeShippingThreshold;
    }

    public void setFreeShippingThreshold(
            BigDecimal freeShippingThreshold) {
        this.freeShippingThreshold = freeShippingThreshold;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    public void setMinimumOrderValue(
            BigDecimal minimumOrderValue) {
        this.minimumOrderValue = minimumOrderValue;
    }
}