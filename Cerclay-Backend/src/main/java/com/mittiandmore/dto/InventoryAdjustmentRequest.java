package com.mittiandmore.dto;

import jakarta.validation.constraints.Min;

public class InventoryAdjustmentRequest {

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer newStock;

    private String reason;

    public Integer getNewStock() { return newStock; }
    public void setNewStock(Integer newStock) { this.newStock = newStock; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
