package com.mittiandmore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String sku;
    private String slug;
    private BigDecimal price;
    private BigDecimal mrp;
    private Integer stock;
    private Boolean active;
    private Boolean archived;
    private Boolean featured;
    private Boolean setOf2Enabled;
    private BigDecimal setOf2Price;
    private BigDecimal setOf2Mrp;
    private String colorGroup;
    private String colorName;
    private String colorHex;
    private BigDecimal rating;
    private Integer reviews;
    private String image;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean value) { this.featured = value; }

    public Boolean getSetOf2Enabled() { return setOf2Enabled; }
    public void setSetOf2Enabled(Boolean value) { this.setOf2Enabled = value; }
    public BigDecimal getSetOf2Price() { return setOf2Price; }
    public void setSetOf2Price(BigDecimal value) { this.setOf2Price = value; }
    public BigDecimal getSetOf2Mrp() { return setOf2Mrp; }
    public void setSetOf2Mrp(BigDecimal value) { this.setOf2Mrp = value; }
    public String getColorGroup() { return colorGroup; }
    public void setColorGroup(String value) { this.colorGroup = value; }
    public String getColorName() { return colorName; }
    public void setColorName(String value) { this.colorName = value; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String value) { this.colorHex = value; }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getReviews() {
        return reviews;
    }

    public void setReviews(Integer reviews) {
        this.reviews = reviews;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<ProductImageResponse> getImages() {
        return images;
    }

    public void setImages(List<ProductImageResponse> images) {
        this.images = images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}