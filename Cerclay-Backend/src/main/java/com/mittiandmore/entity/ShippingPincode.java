package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "shipping_pincodes",
    indexes = {
        @Index(name = "idx_shipping_pincodes_pincode", columnList = "pincode", unique = true),
        @Index(name = "idx_shipping_pincodes_zone", columnList = "zone_id"),
        @Index(name = "idx_shipping_pincodes_active", columnList = "active"),
    }
)
public class ShippingPincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String pincode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private ShippingZone zone;

    @Column(name = "customer_charge_override", precision = 10, scale = 2)
    private BigDecimal customerChargeOverride;

    @Column(name = "free_delivery_threshold_override", precision = 10, scale = 2)
    private BigDecimal freeDeliveryThresholdOverride;

    @Column(name = "cod_charge_override", precision = 10, scale = 2)
    private BigDecimal codChargeOverride;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ShippingPincode() {}

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public ShippingZone getZone() {
        return zone;
    }

    public void setZone(ShippingZone zone) {
        this.zone = zone;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
