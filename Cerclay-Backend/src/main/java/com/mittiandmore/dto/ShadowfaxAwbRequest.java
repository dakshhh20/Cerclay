package com.mittiandmore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShadowfaxAwbRequest {

    @NotBlank(message = "Shadowfax AWB is required")
    @Size(max = 100, message = "Shadowfax AWB must be at most 100 characters")
    private String awb;

    public String getAwb() {
        return awb;
    }

    public void setAwb(String awb) {
        this.awb = awb;
    }
}
