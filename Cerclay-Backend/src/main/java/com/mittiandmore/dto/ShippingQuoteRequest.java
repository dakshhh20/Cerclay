package com.mittiandmore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class ShippingQuoteRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Pincode must contain exactly 6 digits")
    private String pincode;

    @DecimalMin("0.00")
    private BigDecimal orderValue = BigDecimal.ZERO;

    @Pattern(regexp = "COD|CASHFREE|RAZORPAY", message = "Payment method must be COD, CASHFREE or RAZORPAY")
    private String paymentMethod = "CASHFREE";

    public ShippingQuoteRequest() {}

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public BigDecimal getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(BigDecimal orderValue) {
        this.orderValue = orderValue;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
