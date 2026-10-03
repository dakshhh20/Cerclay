package com.mittiandmore.dto;

import jakarta.validation.constraints.Size;

public class CancelOrderRequest {

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
