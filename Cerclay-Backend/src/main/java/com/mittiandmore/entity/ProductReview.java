package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "product_reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_product_review_customer_product",
        columnNames = { "customer_id", "product_id" }
    ),
    indexes = {
        @Index(name = "idx_product_reviews_product_status", columnList = "product_id,status"),
        @Index(name = "idx_product_reviews_customer", columnList = "customer_id"),
    }
)
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 2000)
    private String review;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "verified_purchase", nullable = false)
    private Boolean verifiedPurchase = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductReview() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product v) {
        product = v;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer v) {
        customer = v;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer v) {
        rating = v;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String v) {
        review = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Boolean getVerifiedPurchase() {
        return verifiedPurchase;
    }

    public void setVerifiedPurchase(Boolean v) {
        verifiedPurchase = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
