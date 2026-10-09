package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "discounts",
    indexes = {
        @Index(name = "idx_discounts_code", columnList = "code", unique = true),
        @Index(name = "idx_discounts_active", columnList = "active"),
    }
)
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String code;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumOrderValue = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Integer usageLimit;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean firstTimeOnly = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        LocalDateTime n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        code = v;
    }

    public String getType() {
        return type;
    }

    public void setType(String v) {
        type = v;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal v) {
        value = v;
    }

    public BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    public void setMinimumOrderValue(BigDecimal v) {
        minimumOrderValue = v;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal v) {
        maxDiscount = v;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime v) {
        startsAt = v;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime v) {
        expiresAt = v;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer v) {
        usageLimit = v;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer v) {
        usageCount = v;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean v) {
        active = v;
    }

    public Boolean getFirstTimeOnly() {
        return firstTimeOnly;
    }

    public void setFirstTimeOnly(Boolean v) {
        firstTimeOnly = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
