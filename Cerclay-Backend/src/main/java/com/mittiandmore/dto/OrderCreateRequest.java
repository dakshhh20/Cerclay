package com.mittiandmore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class OrderCreateRequest {

    @NotNull(message = "Address ID is required")
    private Long addressId;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "COD|CASHFREE|RAZORPAY",
            message = "Payment method must be COD, CASHFREE or RAZORPAY"
    )
    private String paymentMethod;

    private String couponCode;

    public OrderCreateRequest() {
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getCouponCode() { return couponCode; }

    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}