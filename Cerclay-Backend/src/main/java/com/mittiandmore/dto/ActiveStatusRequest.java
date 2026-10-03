package com.mittiandmore.dto;
import jakarta.validation.constraints.NotNull;
public class ActiveStatusRequest { @NotNull private Boolean active; public ActiveStatusRequest(){} public Boolean getActive(){return active;} public void setActive(Boolean active){this.active=active;} }
