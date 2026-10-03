package com.mittiandmore.dto;

public class CartItemResponse {
    private Long id;
    private int quantity;
    private int packSize;
    private CartProductResponse product;

    public CartItemResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getQuantity() { return quantity; }
    public int getPackSize() { return packSize; }
    public void setPackSize(int packSize) { this.packSize = packSize; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public CartProductResponse getProduct() { return product; }
    public void setProduct(CartProductResponse product) { this.product = product; }
}
