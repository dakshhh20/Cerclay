package com.mittiandmore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DevelopmentOtpDeliveryService
        implements OtpDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DevelopmentOtpDeliveryService.class
            );

    @Override
    public void sendOtp(
            String destination,
            String destinationType,
            String otp
    ) {

        log.warn(
                "DEVELOPMENT OTP | type={} | destination={} | otp={}",
                destinationType,
                destination,
                otp
        );
    }
}