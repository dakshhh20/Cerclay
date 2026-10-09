package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "refunds",
    indexes = {
        @Index(name = "idx_refunds_order", columnList = "order_id"),
        @Index(name = "idx_refunds_return", columnList = "return_request_id"),
        @Index(name = "idx_refunds_payment", columnList = "payment_id"),
        @Index(name = "idx_refunds_status", columnList = "status"),
    }
)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "razorpay_refund_id", unique = true, length = 100)
    private String razorpayRefundId;

    @Column(name = "cashfree_refund_id", unique = true, length = 100)
    private String cashfreeRefundId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, unique = true, length = 100)
    private String receipt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(ReturnRequest v) {
        returnRequest = v;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order v) {
        order = v;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment v) {
        payment = v;
    }

    public String getRazorpayRefundId() {
        return razorpayRefundId;
    }

    public void setRazorpayRefundId(String v) {
        razorpayRefundId = v;
    }

    public String getCashfreeRefundId() {
        return cashfreeRefundId;
    }

    public void setCashfreeRefundId(String v) {
        cashfreeRefundId = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String v) {
        currency = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String v) {
        receipt = v;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String v) {
        failureReason = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime v) {
        processedAt = v;
    }
}
