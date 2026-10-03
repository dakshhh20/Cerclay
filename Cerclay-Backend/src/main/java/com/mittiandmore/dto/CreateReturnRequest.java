package com.mittiandmore.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*;
public class CreateReturnRequest {
 @NotBlank private String reason; @Size(max=1000) private String customerNote; @NotEmpty @Valid private List<ReturnItemRequest> items = new ArrayList<>();
 public String getReason(){return reason;} public void setReason(String v){reason=v;} public String getCustomerNote(){return customerNote;} public void setCustomerNote(String v){customerNote=v;} public List<ReturnItemRequest> getItems(){return items;} public void setItems(List<ReturnItemRequest> v){items=v;}
}
