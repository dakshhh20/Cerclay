package com.mittiandmore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShippingZoneResponse {
    private Long id;
    private String code;
    private String name;
    private BigDecimal shadowfaxBaseRate;
    private BigDecimal customerCharge;
    private BigDecimal freeDeliveryThreshold;
    private BigDecimal codCharge;
    private Boolean active;
    private LocalDateTime updatedAt;

    public ShippingZoneResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getShadowfaxBaseRate() { return shadowfaxBaseRate; }
    public void setShadowfaxBaseRate(BigDecimal value) { this.shadowfaxBaseRate = value; }
    public BigDecimal getCustomerCharge() { return customerCharge; }
    public void setCustomerCharge(BigDecimal value) { this.customerCharge = value; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal value) { this.freeDeliveryThreshold = value; }
    public BigDecimal getCodCharge() { return codCharge; }
    public void setCodCharge(BigDecimal value) { this.codCharge = value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
