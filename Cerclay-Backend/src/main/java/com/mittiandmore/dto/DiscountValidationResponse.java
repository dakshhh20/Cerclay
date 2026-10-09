package com.mittiandmore.dto;

import java.math.BigDecimal;

public class DiscountValidationResponse {

    private String code;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;

    public DiscountValidationResponse() {}

    public DiscountValidationResponse(String code, BigDecimal discountAmount, BigDecimal subtotal) {
        this.code = code;
        this.discountAmount = discountAmount;
        this.subtotal = subtotal;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
