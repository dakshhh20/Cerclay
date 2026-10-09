package com.mittiandmore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProductReviewRequest {

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 2000)
    private String review;

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
}
