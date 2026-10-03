package com.mittiandmore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShippingPincodeResponse {
    private Long id;
    private String pincode;
    private Long zoneId;
    private String zoneCode;
    private String zoneName;
    private BigDecimal customerChargeOverride;
    private BigDecimal freeDeliveryThresholdOverride;
    private BigDecimal codChargeOverride;
    private Boolean active;
    private LocalDateTime updatedAt;

    public ShippingPincodeResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public BigDecimal getCustomerChargeOverride() { return customerChargeOverride; }
    public void setCustomerChargeOverride(BigDecimal value) { this.customerChargeOverride = value; }
    public BigDecimal getFreeDeliveryThresholdOverride() { return freeDeliveryThresholdOverride; }
    public void setFreeDeliveryThresholdOverride(BigDecimal value) { this.freeDeliveryThresholdOverride = value; }
    public BigDecimal getCodChargeOverride() { return codChargeOverride; }
    public void setCodChargeOverride(BigDecimal value) { this.codChargeOverride = value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
