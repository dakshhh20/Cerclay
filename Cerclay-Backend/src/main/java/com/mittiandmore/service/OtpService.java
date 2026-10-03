package com.mittiandmore.service;

import com.mittiandmore.entity.CustomerOtpVerification;
import com.mittiandmore.repository.CustomerOtpVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final CustomerOtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            CustomerOtpVerificationRepository otpRepository,
            PasswordEncoder passwordEncoder,
            OtpDeliveryService otpDeliveryService
    ) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpDeliveryService = otpDeliveryService;
    }

    @Transactional
    public void generateAndSendOtp(
            String destination,
            String destinationType,
            String purpose
    ) {

        validateDestination(
                destination,
                destinationType
        );

        validatePurpose(purpose);

        LocalDateTime now = LocalDateTime.now();

        CustomerOtpVerification existingOtp =
                otpRepository
                        .findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                destination,
                                purpose
                        )
                        .orElse(null);

        if (existingOtp != null) {

            LocalDateTime cooldownEndsAt =
                    existingOtp.getLastSentAt()
                            .plusSeconds(RESEND_COOLDOWN_SECONDS);

            if (now.isBefore(cooldownEndsAt)) {

                long secondsRemaining =
                        java.time.Duration
                                .between(now, cooldownEndsAt)
                                .getSeconds();

                throw new IllegalStateException(
                        "Please wait "
                                + Math.max(secondsRemaining, 1)
                                + " seconds before requesting another OTP"
                );
            }
        }

        invalidatePreviousOtps(
                destination,
                purpose
        );

        String otp = generateOtp();

        String otpHash =
                passwordEncoder.encode(otp);

        CustomerOtpVerification verification =
                new CustomerOtpVerification();

        verification.setDestination(destination);
        verification.setDestinationType(destinationType);
        verification.setPurpose(purpose);
        verification.setOtpHash(otpHash);

        verification.setExpiresAt(
                now.plusMinutes(OTP_EXPIRY_MINUTES)
        );

        verification.setVerifiedAt(null);
        verification.setAttemptCount(0);
        verification.setMaxAttempts(MAX_ATTEMPTS);
        verification.setLastSentAt(now);
        verification.setCreatedAt(now);
        verification.setUpdatedAt(now);

        otpRepository.save(verification);

        /*
         * The delivery layer only handles delivery.
         * OTP purpose remains part of the OTP service logic.
         */
        otpDeliveryService.sendOtp(
                destination,
                destinationType,
                otp
        );
    }

    @Transactional
    public void verifyOtp(
            String destination,
            String purpose,
            String otp
    ) {

        if (destination == null || destination.isBlank()) {

            throw new IllegalArgumentException(
                    "Destination is required"
            );
        }

        if (purpose == null || purpose.isBlank()) {

            throw new IllegalArgumentException(
                    "OTP purpose is required"
            );
        }

        if (otp == null || !otp.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must be a 6-digit number"
            );
        }

        CustomerOtpVerification verification =
                otpRepository
                        .findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                destination,
                                purpose
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "OTP not found or already used"
                                )
                        );

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(verification.getExpiresAt())) {

            verification.setVerifiedAt(now);
            verification.setUpdatedAt(now);

            otpRepository.save(verification);

            throw new IllegalArgumentException(
                    "OTP has expired"
            );
        }

        if (verification.getAttemptCount()
                >= verification.getMaxAttempts()) {

            verification.setVerifiedAt(now);
            verification.setUpdatedAt(now);

            otpRepository.save(verification);

            throw new IllegalArgumentException(
                    "Maximum OTP attempts exceeded"
            );
        }

        verification.setAttemptCount(
                verification.getAttemptCount() + 1
        );

        boolean matches =
                passwordEncoder.matches(
                        otp,
                        verification.getOtpHash()
                );

        if (!matches) {

            if (verification.getAttemptCount()
                    >= verification.getMaxAttempts()) {

                verification.setVerifiedAt(now);
            }

            verification.setUpdatedAt(now);

            otpRepository.save(verification);

            throw new IllegalArgumentException(
                    verification.getVerifiedAt() != null
                            ? "Maximum OTP attempts exceeded"
                            : "Invalid OTP"
            );
        }

        verification.setVerifiedAt(now);
        verification.setUpdatedAt(now);

        otpRepository.save(verification);
    }

    private void invalidatePreviousOtps(
            String destination,
            String purpose
    ) {

        List<CustomerOtpVerification> previousOtps =
                otpRepository
                        .findByDestinationAndPurposeAndVerifiedAtIsNull(
                                destination,
                                purpose
                        );

        if (previousOtps.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (CustomerOtpVerification otp : previousOtps) {

            otp.setVerifiedAt(now);
            otp.setUpdatedAt(now);
        }

        otpRepository.saveAll(previousOtps);
    }

    private String generateOtp() {

        int minimum = 100000;
        int maximum = 999999;

        int otp =
                secureRandom.nextInt(
                        maximum - minimum + 1
                ) + minimum;

        return String.valueOf(otp);
    }

    private void validateDestination(
            String destination,
            String destinationType
    ) {

        if (destination == null
                || destination.isBlank()) {

            throw new IllegalArgumentException(
                    "OTP destination is required"
            );
        }

        if (destinationType == null
                || destinationType.isBlank()) {

            throw new IllegalArgumentException(
                    "OTP destination type is required"
            );
        }

        if (!destinationType.equals("PHONE")
                && !destinationType.equals("EMAIL")) {

            throw new IllegalArgumentException(
                    "OTP destination type must be PHONE or EMAIL"
            );
        }
    }

    private void validatePurpose(String purpose) {

        switch (purpose) {

            case "PHONE_LOGIN",
                 "PHONE_VERIFICATION",
                 "PASSWORD_RESET",
                 "EMAIL_VERIFICATION" -> {
            }

            default -> throw new IllegalArgumentException(
                    "Invalid OTP purpose"
            );
        }
    }
}