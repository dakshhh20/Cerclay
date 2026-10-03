package com.mittiandmore.dto;

import jakarta.validation.constraints.NotBlank;

public class RazorpayVerifyRequest {
    @NotBlank private String razorpayOrderId;
    @NotBlank private String razorpayPaymentId;
    @NotBlank private String razorpaySignature;

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String value) { this.razorpayOrderId = value; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String value) { this.razorpayPaymentId = value; }
    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String value) { this.razorpaySignature = value; }
}
