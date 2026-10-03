package com.mittiandmore.dto;

import jakarta.validation.constraints.NotBlank;

public class SendOtpRequest {

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Destination type is required")
    private String destinationType;

    @NotBlank(message = "OTP purpose is required")
    private String purpose;

    public SendOtpRequest() {
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}