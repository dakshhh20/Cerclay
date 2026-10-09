package com.mittiandmore.dto;

import java.math.BigDecimal;

public class ShippingQuoteResponse {

    private String pincode;
    private boolean serviceable;
    private String service;
    private String zoneCode;
    private String zoneName;
    private BigDecimal shadowfaxBaseRate;
    private BigDecimal customerShippingCharge;
    private BigDecimal codCharge;
    private BigDecimal freeDeliveryThreshold;
    private boolean freeDeliveryApplied;
    private String message;

    public ShippingQuoteResponse() {}

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public boolean isServiceable() {
        return serviceable;
    }

    public void setServiceable(boolean serviceable) {
        this.serviceable = serviceable;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public void setZoneCode(String zoneCode) {
        this.zoneCode = zoneCode;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public BigDecimal getShadowfaxBaseRate() {
        return shadowfaxBaseRate;
    }

    public void setShadowfaxBaseRate(BigDecimal value) {
        this.shadowfaxBaseRate = value;
    }

    public BigDecimal getCustomerShippingCharge() {
        return customerShippingCharge;
    }

    public void setCustomerShippingCharge(BigDecimal value) {
        this.customerShippingCharge = value;
    }

    public BigDecimal getCodCharge() {
        return codCharge;
    }

    public void setCodCharge(BigDecimal value) {
        this.codCharge = value;
    }

    public BigDecimal getFreeDeliveryThreshold() {
        return freeDeliveryThreshold;
    }

    public void setFreeDeliveryThreshold(BigDecimal value) {
        this.freeDeliveryThreshold = value;
    }

    public boolean isFreeDeliveryApplied() {
        return freeDeliveryApplied;
    }

    public void setFreeDeliveryApplied(boolean value) {
        this.freeDeliveryApplied = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
