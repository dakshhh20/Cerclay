package com.mittiandmore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordResetRequest {

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Destination type is required")
    @Pattern(
            regexp = "EMAIL|PHONE",
            message = "Destination type must be EMAIL or PHONE"
    )
    private String destinationType;

    @NotBlank(message = "OTP is required")
    @Pattern(
            regexp = "\\d{6}",
            message = "OTP must contain exactly 6 digits"
    )
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(
            min = 8,
            max = 100,
            message = "Password must be between 8 and 100 characters"
    )
    private String newPassword;

    public PasswordResetRequest() {
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}