package com.mittiandmore.dto;

import com.mittiandmore.entity.Discount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicDiscountResponse {

    private String code, type;
    private BigDecimal value, minimumOrderValue, maxDiscount;
    private LocalDateTime startsAt, expiresAt;
    private Boolean firstTimeOnly;

    public static PublicDiscountResponse from(Discount d) {
        PublicDiscountResponse r = new PublicDiscountResponse();
        r.code = d.getCode();
        r.type = d.getType();
        r.value = d.getValue();
        r.minimumOrderValue = d.getMinimumOrderValue();
        r.maxDiscount = d.getMaxDiscount();
        r.startsAt = d.getStartsAt();
        r.expiresAt = d.getExpiresAt();
        r.firstTimeOnly = Boolean.TRUE.equals(d.getFirstTimeOnly());
        return r;
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

    public Boolean getFirstTimeOnly() {
        return firstTimeOnly;
    }
}
