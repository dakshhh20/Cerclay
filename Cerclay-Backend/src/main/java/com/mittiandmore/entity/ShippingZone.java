package com.mittiandmore.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipping_zones",
        indexes = {
                @Index(name = "idx_shipping_zones_code", columnList = "code", unique = true),
                @Index(name = "idx_shipping_zones_active", columnList = "active")
        }
)
public class ShippingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "shadowfax_base_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal shadowfaxBaseRate = BigDecimal.ZERO;

    @Column(name = "customer_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal customerCharge = BigDecimal.ZERO;

    @Column(name = "free_delivery_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold = BigDecimal.ZERO;

    @Column(name = "cod_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal codCharge = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ShippingZone() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getShadowfaxBaseRate() { return shadowfaxBaseRate; }
    public void setShadowfaxBaseRate(BigDecimal shadowfaxBaseRate) { this.shadowfaxBaseRate = shadowfaxBaseRate; }
    public BigDecimal getCustomerCharge() { return customerCharge; }
    public void setCustomerCharge(BigDecimal customerCharge) { this.customerCharge = customerCharge; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }
    public BigDecimal getCodCharge() { return codCharge; }
    public void setCodCharge(BigDecimal codCharge) { this.codCharge = codCharge; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
