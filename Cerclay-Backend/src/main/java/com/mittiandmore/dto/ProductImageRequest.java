package com.mittiandmore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProductImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 2000, message = "Image URL must not exceed 2000 characters")
    private String imageUrl;

    private Boolean primary = false;

    private Integer displayOrder = 0;

    public ProductImageRequest() {}

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
