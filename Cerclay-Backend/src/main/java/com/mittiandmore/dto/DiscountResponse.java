package com.mittiandmore.dto;

import com.mittiandmore.entity.Discount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DiscountResponse {

    private Long id;
    private String code, type;
    private BigDecimal value, minimumOrderValue, maxDiscount;
    private LocalDateTime startsAt, expiresAt;
    private Integer usageLimit, usageCount;
    private Boolean active, firstTimeOnly;

    public static DiscountResponse from(Discount d) {
        DiscountResponse r = new DiscountResponse();
        r.id = d.getId();
        r.code = d.getCode();
        r.type = d.getType();
        r.value = d.getValue();
        r.minimumOrderValue = d.getMinimumOrderValue();
        r.maxDiscount = d.getMaxDiscount();
        r.startsAt = d.getStartsAt();
        r.expiresAt = d.getExpiresAt();
        r.usageLimit = d.getUsageLimit();
        r.usageCount = d.getUsageCount();
        r.active = d.getActive();
        r.firstTimeOnly = d.getFirstTimeOnly();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getFirstTimeOnly() {
        return firstTimeOnly;
    }
}
