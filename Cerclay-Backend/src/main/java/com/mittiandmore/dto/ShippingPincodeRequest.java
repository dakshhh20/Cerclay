package com.mittiandmore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class ShippingPincodeRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Pincode must contain exactly 6 digits")
    private String pincode;

    @NotNull
    private Long zoneId;

    @DecimalMin("0.00")
    private BigDecimal customerChargeOverride;

    @DecimalMin("0.00")
    private BigDecimal freeDeliveryThresholdOverride;

    @DecimalMin("0.00")
    private BigDecimal codChargeOverride;

    private Boolean active = true;

    public ShippingPincodeRequest() {}

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public BigDecimal getCustomerChargeOverride() {
        return customerChargeOverride;
    }

    public void setCustomerChargeOverride(BigDecimal value) {
        this.customerChargeOverride = value;
    }

    public BigDecimal getFreeDeliveryThresholdOverride() {
        return freeDeliveryThresholdOverride;
    }

    public void setFreeDeliveryThresholdOverride(BigDecimal value) {
        this.freeDeliveryThresholdOverride = value;
    }

    public BigDecimal getCodChargeOverride() {
        return codChargeOverride;
    }

    public void setCodChargeOverride(BigDecimal value) {
        this.codChargeOverride = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
