package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "return_requests",
    indexes = {
        @Index(name = "idx_return_requests_order", columnList = "order_id"),
        @Index(name = "idx_return_requests_customer", columnList = "customer_id"),
        @Index(name = "idx_return_requests_status", columnList = "status"),
    }
)
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "customer_note", length = 1000)
    private String customerNote;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "refund_requested_amount", precision = 10, scale = 2)
    private BigDecimal refundRequestedAmount;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnPhoto> photos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order v) {
        order = v;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer v) {
        customer = v;
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

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String v) {
        customerNote = v;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String v) {
        adminNote = v;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime v) {
        requestedAt = v;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime v) {
        approvedAt = v;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime v) {
        rejectedAt = v;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime v) {
        receivedAt = v;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime v) {
        closedAt = v;
    }

    public BigDecimal getRefundRequestedAmount() {
        return refundRequestedAmount;
    }

    public void setRefundRequestedAmount(BigDecimal v) {
        refundRequestedAmount = v;
    }

    public List<ReturnItem> getItems() {
        return items;
    }

    public void setItems(List<ReturnItem> v) {
        items = v;
    }

    public List<ReturnPhoto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<ReturnPhoto> v) {
        photos = v;
    }
}
