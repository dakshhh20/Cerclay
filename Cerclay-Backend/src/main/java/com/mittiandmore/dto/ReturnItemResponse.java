package com.mittiandmore.dto;

import java.math.BigDecimal;

public class ReturnItemResponse {

    private Long id, orderItemId, productId;
    private String productName, sku;
    private Integer quantity;
    private BigDecimal unitPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long v) {
        orderItemId = v;
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

    public String getSku() {
        return sku;
    }

    public void setSku(String v) {
        sku = v;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer v) {
        quantity = v;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal v) {
        unitPrice = v;
    }
}
