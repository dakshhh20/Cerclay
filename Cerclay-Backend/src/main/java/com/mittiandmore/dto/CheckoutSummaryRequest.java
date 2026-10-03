package com.mittiandmore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CheckoutSummaryRequest {
    @NotNull private Long addressId;
    @Pattern(regexp = "COD|CASHFREE|RAZORPAY", message = "Payment method must be COD, CASHFREE or RAZORPAY")
    private String paymentMethod = "CASHFREE";
    private String couponCode;

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
