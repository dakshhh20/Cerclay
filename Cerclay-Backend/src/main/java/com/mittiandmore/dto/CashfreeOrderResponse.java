package com.mittiandmore.dto;

import java.math.BigDecimal;

public class CashfreeOrderResponse {

    private Long orderId;
    private String orderNumber;
    private String cashfreeOrderId;
    private String paymentSessionId;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String environment;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCashfreeOrderId() {
        return cashfreeOrderId;
    }

    public void setCashfreeOrderId(String cashfreeOrderId) {
        this.cashfreeOrderId = cashfreeOrderId;
    }

    public String getPaymentSessionId() {
        return paymentSessionId;
    }

    public void setPaymentSessionId(String paymentSessionId) {
        this.paymentSessionId = paymentSessionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
