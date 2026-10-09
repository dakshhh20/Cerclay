package com.mittiandmore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @NotBlank(message = "Slug is required")
    @Size(max = 180, message = "Slug must not exceed 180 characters")
    private String slug;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be zero or greater")
    private BigDecimal price;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.00", message = "MRP must be zero or greater")
    private BigDecimal mrp;

    @NotNull(message = "Stock is required")
    private Integer stock;

    private Boolean active = true;
    private Boolean featured = false;
    private Boolean setOf2Enabled = false;

    @DecimalMin(value = "0.00", message = "Set of 2 price must be zero or greater")
    private BigDecimal setOf2Price;

    @DecimalMin(value = "0.00", message = "Set of 2 MRP must be zero or greater")
    private BigDecimal setOf2Mrp;

    @Size(max = 150, message = "Color variant group must not exceed 150 characters")
    private String colorGroup;

    @Size(max = 80, message = "Color name must not exceed 80 characters")
    private String colorName;

    @Size(max = 7, message = "Color swatch must be a hex color such as #C45A4A")
    private String colorHex;

    public ProductRequest() {}

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

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean value) {
        this.featured = value;
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

    public BigDecimal getSetOf2Mrp() {
        return setOf2Mrp;
    }

    public void setSetOf2Mrp(BigDecimal value) {
        this.setOf2Mrp = value;
    }

    public String getColorGroup() {
        return colorGroup;
    }

    public void setColorGroup(String value) {
        this.colorGroup = value;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String value) {
        this.colorName = value;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String value) {
        this.colorHex = value;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
