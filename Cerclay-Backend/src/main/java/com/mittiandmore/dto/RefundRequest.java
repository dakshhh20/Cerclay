package com.mittiandmore.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public class RefundRequest { @NotNull @DecimalMin("0.01") private BigDecimal amount; @Size(max=500) private String reason;
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;}
}
