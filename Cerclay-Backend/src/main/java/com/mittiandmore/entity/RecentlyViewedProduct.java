package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "recently_viewed_products",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_recently_viewed_customer_product", columnNames = { "customer_id", "product_id" }),
    },
    indexes = {
        @Index(name = "idx_recently_viewed_customer", columnList = "customer_id"),
        @Index(name = "idx_recently_viewed_customer_last_viewed", columnList = "customer_id,last_viewed_at"),
        @Index(name = "idx_recently_viewed_product", columnList = "product_id"),
    }
)
public class RecentlyViewedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    public RecentlyViewedProduct() {}

    @PrePersist
    protected void onCreate() {
        if (lastViewedAt == null) {
            lastViewedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LocalDateTime getLastViewedAt() {
        return lastViewedAt;
    }

    public void setLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }
}
