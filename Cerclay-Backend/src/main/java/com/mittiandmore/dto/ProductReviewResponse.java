package com.mittiandmore.dto;

import java.time.LocalDateTime;

public class ProductReviewResponse {

    private Long id, productId;
    private String productName, customerName, review, status;
    private Integer rating;
    private Boolean verifiedPurchase;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long v) {
        productId = v;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String v) {
        productName = v;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String v) {
        customerName = v;
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer v) {
        rating = v;
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

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
