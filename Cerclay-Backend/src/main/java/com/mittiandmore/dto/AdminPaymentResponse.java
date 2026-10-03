package com.mittiandmore.dto;
import com.mittiandmore.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class AdminPaymentResponse {
 private Long id,orderId; private String orderNumber,razorpayOrderId,razorpayPaymentId,cashfreeOrderId,cashfreePaymentId,currency,paymentStatus,paymentMethod; private BigDecimal amount; private LocalDateTime createdAt,updatedAt;
 public static AdminPaymentResponse from(Payment p){AdminPaymentResponse r=new AdminPaymentResponse();r.id=p.getId();r.orderId=p.getOrder().getId();r.orderNumber=p.getOrder().getOrderNumber();r.razorpayOrderId=p.getRazorpayOrderId();r.razorpayPaymentId=p.getRazorpayPaymentId();r.cashfreeOrderId=p.getCashfreeOrderId();r.cashfreePaymentId=p.getCashfreePaymentId();r.currency=p.getCurrency();r.paymentStatus=p.getPaymentStatus();r.paymentMethod=p.getPaymentMethod();r.amount=p.getAmount();r.createdAt=p.getCreatedAt();r.updatedAt=p.getUpdatedAt();return r;}
 public Long getId(){return id;} public Long getOrderId(){return orderId;} public String getOrderNumber(){return orderNumber;} public String getRazorpayOrderId(){return razorpayOrderId;} public String getRazorpayPaymentId(){return razorpayPaymentId;} public String getCashfreeOrderId(){return cashfreeOrderId;} public String getCashfreePaymentId(){return cashfreePaymentId;} public String getCurrency(){return currency;} public String getPaymentStatus(){return paymentStatus;} public String getPaymentMethod(){return paymentMethod;} public BigDecimal getAmount(){return amount;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
