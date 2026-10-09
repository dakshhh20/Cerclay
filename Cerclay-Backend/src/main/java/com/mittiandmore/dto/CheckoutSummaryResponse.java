package com.mittiandmore.dto;

import java.math.BigDecimal;

public class CheckoutSummaryResponse {

    private BigDecimal subtotal;
    private BigDecimal productDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal gst;
    private BigDecimal shippingCharge;
    private BigDecimal total;
    private String paymentMethod;
    private String couponCode;
    private boolean serviceable;
    private String shippingZone;

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal v) {
        subtotal = v;
    }

    public BigDecimal getProductDiscount() {
        return productDiscount;
    }

    public void setProductDiscount(BigDecimal v) {
        productDiscount = v;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(BigDecimal v) {
        couponDiscount = v;
    }

    public BigDecimal getGst() {
        return gst;
    }

    public void setGst(BigDecimal v) {
        gst = v;
    }

    public BigDecimal getShippingCharge() {
        return shippingCharge;
    }

    public void setShippingCharge(BigDecimal v) {
        shippingCharge = v;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal v) {
        total = v;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String v) {
        paymentMethod = v;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String v) {
        couponCode = v;
    }

    public boolean isServiceable() {
        return serviceable;
    }

    public void setServiceable(boolean v) {
        serviceable = v;
    }

    public String getShippingZone() {
        return shippingZone;
    }

    public void setShippingZone(String v) {
        shippingZone = v;
    }
}
