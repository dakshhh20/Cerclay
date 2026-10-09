package com.mittiandmore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ShippingZoneRequest {

    @NotBlank
    @Size(max = 20)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal shadowfaxBaseRate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal customerCharge;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal freeDeliveryThreshold;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal codCharge;

    private Boolean active = true;

    public ShippingZoneRequest() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getShadowfaxBaseRate() {
        return shadowfaxBaseRate;
    }

    public void setShadowfaxBaseRate(BigDecimal value) {
        this.shadowfaxBaseRate = value;
    }

    public BigDecimal getCustomerCharge() {
        return customerCharge;
    }

    public void setCustomerCharge(BigDecimal value) {
        this.customerCharge = value;
    }

    public BigDecimal getFreeDeliveryThreshold() {
        return freeDeliveryThreshold;
    }

    public void setFreeDeliveryThreshold(BigDecimal value) {
        this.freeDeliveryThreshold = value;
    }

    public BigDecimal getCodCharge() {
        return codCharge;
    }

    public void setCodCharge(BigDecimal value) {
        this.codCharge = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
