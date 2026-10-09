package com.mittiandmore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReturnItemRequest {

    @NotNull
    private Long orderItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long v) {
        orderItemId = v;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer v) {
        quantity = v;
    }
}
