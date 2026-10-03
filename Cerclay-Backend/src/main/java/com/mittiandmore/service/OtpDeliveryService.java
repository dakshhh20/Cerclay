package com.mittiandmore.service;

public interface OtpDeliveryService {

    void sendOtp(
            String destination,
            String destinationType,
            String otp
    );
}