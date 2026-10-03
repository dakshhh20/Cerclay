package com.mittiandmore.dto;
import jakarta.validation.constraints.Size;
public class ReturnDecisionRequest { @Size(max=1000) private String note; public String getNote(){return note;} public void setNote(String v){note=v;} }
