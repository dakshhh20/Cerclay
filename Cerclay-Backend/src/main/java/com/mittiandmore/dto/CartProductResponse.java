package com.mittiandmore.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartProductResponse {

    private Long id;
    private String name;
    private String category;
    private String sku;
    private String slug;
    private BigDecimal price;
    private BigDecimal mrp;
    private Integer stock;
    private Boolean active;
    private Boolean setOf2Enabled;
    private BigDecimal setOf2Price;
    private String image;
    private List<ProductImageResponse> images;

    public CartProductResponse() {}

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

    public Boolean getSetOf2Enabled() {
        return setOf2Enabled;
    }

    public void setSetOf2Enabled(Boolean value) {
        this.setOf2Enabled = value;
    }

    public BigDecimal getSetOf2Price() {
        return setOf2Price;
    }

    public void setSetOf2Price(BigDecimal value) {
        this.setOf2Price = value;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
}
