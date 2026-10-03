package com.mittiandmore.dto;
import jakarta.validation.constraints.NotBlank;
public class OrderStatusRequest { @NotBlank private String status; public OrderStatusRequest(){} public String getStatus(){return status;} public void setStatus(String status){this.status=status;} }
