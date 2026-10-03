package com.mittiandmore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "return_items", uniqueConstraints = @UniqueConstraint(name = "uk_return_item_request_order_item", columnNames = {"return_request_id", "order_item_id"}))
public class ReturnItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;
    @Column(nullable = false) private Integer quantity;

    public Long getId(){return id;}
    public ReturnRequest getReturnRequest(){return returnRequest;} public void setReturnRequest(ReturnRequest v){returnRequest=v;}
    public OrderItem getOrderItem(){return orderItem;} public void setOrderItem(OrderItem v){orderItem=v;}
    public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;}
}
