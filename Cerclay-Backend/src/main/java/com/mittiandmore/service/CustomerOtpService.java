package com.mittiandmore.service;

import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.CustomerOtpVerification;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CustomerOtpVerificationRepository;
import com.mittiandmore.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CustomerOtpService {

    private static final int OTP_MIN = 100000;
    private static final int OTP_RANGE = 900000;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");

    private final CustomerRepository customerRepository;
    private final CustomerOtpVerificationRepository otpRepository;
    private final OtpDeliveryService otpDeliveryService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.otp.expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.auth.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.auth.otp.max-attempts:5}")
    private int maxAttempts;

    public CustomerOtpService(
            CustomerRepository customerRepository,
            CustomerOtpVerificationRepository otpRepository,
            OtpDeliveryService otpDeliveryService,
            PasswordEncoder passwordEncoder
    ) {
        this.customerRepository = customerRepository;
        this.otpRepository = otpRepository;
        this.otpDeliveryService = otpDeliveryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendOtp(
            String destination,
            String destinationType,
            String purpose
    ) {

        String normalizedDestination =
                normalizeDestination(destination, destinationType);

        String normalizedType =
                normalizeDestinationType(destinationType);

        String normalizedPurpose =
                normalizePurpose(purpose);

        validatePurposeAndDestinationType(
                normalizedPurpose,
                normalizedType
        );

        LocalDateTime now = LocalDateTime.now();

        Optional<CustomerOtpVerification> latestOtp =
                otpRepository
                        .findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                normalizedDestination,
                                normalizedPurpose
                        );

        if (latestOtp.isPresent()) {

            LocalDateTime lastSentAt =
                    latestOtp.get().getLastSentAt();

            if (lastSentAt != null
                    && lastSentAt
                    .plusSeconds(resendCooldownSeconds)
                    .isAfter(now)) {

                throw new ApiException(
                        "OTP_RESEND_TOO_SOON",
                        "Please wait before requesting another OTP",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
        }

        Customer customer =
                findCustomerForDestination(
                        normalizedDestination,
                        normalizedType
                );

        /*
         * PASSWORD_RESET must not reveal whether an account exists.
         *
         * If there is no matching customer, simply return without
         * creating or sending an OTP.
         */
        if ("PASSWORD_RESET".equals(normalizedPurpose)
                && customer == null) {

            return;
        }

        String otp = generateOtp();

        CustomerOtpVerification verification =
                new CustomerOtpVerification();

        verification.setCustomer(customer);
        verification.setDestination(normalizedDestination);
        verification.setDestinationType(normalizedType);
        verification.setPurpose(normalizedPurpose);
        verification.setOtpHash(
                passwordEncoder.encode(otp)
        );
        verification.setExpiresAt(
                now.plusMinutes(otpExpiryMinutes)
        );
        verification.setAttemptCount(0);
        verification.setMaxAttempts(maxAttempts);
        verification.setLastSentAt(now);
        verification.setCreatedAt(now);
        verification.setUpdatedAt(now);

        otpRepository.save(verification);

        otpDeliveryService.sendOtp(
                normalizedDestination,
                normalizedType,
                otp
        );
    }

    @Transactional(noRollbackFor = ApiException.class)
    public CustomerOtpVerification verifyOtp(
            String destination,
            String destinationType,
            String purpose,
            String otp
    ) {

        String normalizedDestination =
                normalizeDestination(destination, destinationType);

        String normalizedType =
                normalizeDestinationType(destinationType);

        String normalizedPurpose =
                normalizePurpose(purpose);

        validatePurposeAndDestinationType(
                normalizedPurpose,
                normalizedType
        );

        if (otp == null || !otp.matches("\\d{6}")) {

            throw new ApiException(
                    "INVALID_OTP",
                    "OTP must contain exactly 6 digits",
                    HttpStatus.BAD_REQUEST
            );
        }

        CustomerOtpVerification verification =
                otpRepository
                        .findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                normalizedDestination,
                                normalizedPurpose
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        "OTP_NOT_FOUND",
                                        "Invalid or expired OTP",
                                        HttpStatus.BAD_REQUEST
                                )
                        );

        LocalDateTime now = LocalDateTime.now();

        if (verification.getExpiresAt().isBefore(now)) {

            throw new ApiException(
                    "OTP_EXPIRED",
                    "OTP has expired",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (verification.getAttemptCount()
                >= verification.getMaxAttempts()) {

            throw new ApiException(
                    "OTP_MAX_ATTEMPTS_EXCEEDED",
                    "Maximum OTP attempts exceeded",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        verification.setAttemptCount(
                verification.getAttemptCount() + 1
        );

        if (!passwordEncoder.matches(
                otp,
                verification.getOtpHash()
        )) {

            otpRepository.save(verification);

            throw new ApiException(
                    "INVALID_OTP",
                    "Invalid OTP",
                    HttpStatus.BAD_REQUEST
            );
        }

        verification.setVerifiedAt(now);
        verification.setUpdatedAt(now);

        Customer customer = verification.getCustomer();

        /*
         * Only verification-related OTP purposes should change
         * the customer's verification flags.
         *
         * PASSWORD_RESET must NOT mark email/phone as verified.
         */
        if (customer != null) {

            if ("PHONE_LOGIN".equals(normalizedPurpose)
                    || "PHONE_VERIFICATION".equals(normalizedPurpose)) {

                customer.setPhoneVerified(true);
            }

            if ("EMAIL_VERIFICATION".equals(normalizedPurpose)) {

                customer.setEmailVerified(true);
            }

            customerRepository.save(customer);
        }

        return otpRepository.save(verification);
    }

    /*
     * PASSWORD RESET
     *
     * Verifies the OTP and immediately changes the password.
     * No separate reset-token entity is required.
     */
    @Transactional
    public Customer resetPassword(
            String destination,
            String destinationType,
            String otp,
            String newPassword
    ) {

        if (newPassword == null
                || newPassword.isBlank()) {

            throw new ApiException(
                    "PASSWORD_REQUIRED",
                    "New password is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (newPassword.length() < 8) {

            throw new ApiException(
                    "PASSWORD_TOO_SHORT",
                    "Password must contain at least 8 characters",
                    HttpStatus.BAD_REQUEST
            );
        }

        CustomerOtpVerification verification =
                verifyOtp(
                        destination,
                        destinationType,
                        "PASSWORD_RESET",
                        otp
                );

        Customer customer =
                verification.getCustomer();

        if (customer == null) {

            throw new ApiException(
                    "INVALID_PASSWORD_RESET",
                    "Invalid password reset request",
                    HttpStatus.BAD_REQUEST
            );
        }

        customer.setPassword(
                passwordEncoder.encode(newPassword)
        );

        Customer updatedCustomer =
                customerRepository.save(customer);

        /*
         * The OTP has already been marked verified by verifyOtp().
         * Therefore it cannot be verified again through the normal
         * unverified-OTP lookup.
         */

        return updatedCustomer;
    }

    /**
     * Verifies a PHONE_LOGIN OTP and supports both existing-customer login
     * and first-time phone signup. For a new phone number, name and email
     * are required and the account is created only after the OTP is valid.
     */
    @Transactional
    public Customer verifyPhoneLoginOtp(
            String destination,
            String otp,
            String name,
            String email
    ) {

        String normalizedPhone =
                normalizeDestination(destination, "PHONE");

        Customer existingCustomer =
                customerRepository
                        .findByPhone(normalizedPhone)
                        .orElse(null);

        if (existingCustomer != null) {
            return verifyOtp(
                    normalizedPhone,
                    "PHONE",
                    "PHONE_LOGIN",
                    otp
            ).getCustomer();
        }

        String normalizedName =
                name == null ? "" : name.trim();

        if (normalizedName.isBlank()) {
            throw new ApiException(
                    "PHONE_ACCOUNT_DETAILS_REQUIRED",
                    "Please provide your full name and email to create your Cerclay account",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (normalizedName.length() > 100) {
            throw new ApiException(
                    "INVALID_NAME",
                    "Name must be 100 characters or fewer",
                    HttpStatus.BAD_REQUEST
            );
        }

        String normalizedEmail =
                normalizeDestination(email, "EMAIL");

        if (customerRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ApiException(
                    "EMAIL_ALREADY_REGISTERED",
                    "That email address is already registered. Please use another email address.",
                    HttpStatus.CONFLICT
            );
        }

        CustomerOtpVerification verification =
                verifyOtp(
                        normalizedPhone,
                        "PHONE",
                        "PHONE_LOGIN",
                        otp
                );

        // The OTP was valid, so this phone number is verified for the new account.
        Customer customer = new Customer();
        customer.setName(normalizedName);
        customer.setEmail(normalizedEmail);
        customer.setPhone(normalizedPhone);
        customer.setPassword(
                passwordEncoder.encode(UUID.randomUUID().toString())
        );
        customer.setActive(true);
        customer.setRole("CUSTOMER");
        customer.setEmailVerified(false);
        customer.setPhoneVerified(true);

        Customer savedCustomer = customerRepository.save(customer);

        verification.setCustomer(savedCustomer);
        otpRepository.save(verification);

        return savedCustomer;
    }

    public Customer getCustomerByEmail(String email) {

        return customerRepository
                .findByEmail(email)
                .orElse(null);
    }

    private String generateOtp() {

        return String.valueOf(
                OTP_MIN + secureRandom.nextInt(OTP_RANGE)
        );
    }

    private Customer findCustomerForDestination(
            String destination,
            String destinationType
    ) {

        if ("PHONE".equals(destinationType)) {

            return customerRepository
                    .findByPhone(destination)
                    .orElse(null);
        }

        if ("EMAIL".equals(destinationType)) {

            return customerRepository
                    .findByEmail(destination)
                    .orElse(null);
        }

        return null;
    }

    private String normalizeDestination(
            String destination,
            String destinationType
    ) {

        if (destination == null
                || destination.isBlank()) {

            throw new ApiException(
                    "DESTINATION_REQUIRED",
                    "Phone number or email is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        String type =
                normalizeDestinationType(destinationType);

        String normalized =
                destination.trim();

        if ("EMAIL".equals(type)) {

            normalized =
                    normalized.toLowerCase(Locale.ROOT);

            if (!EMAIL_PATTERN.matcher(normalized).matches()) {

                throw new ApiException(
                        "INVALID_EMAIL",
                        "Invalid email address",
                        HttpStatus.BAD_REQUEST
                );
            }

            return normalized;
        }

        if ("PHONE".equals(type)) {

            normalized =
                    normalized.replaceAll("[\\s()-]", "");

            if (normalized.startsWith("+91")) {

                normalized =
                        normalized.substring(3);

            } else if (normalized.startsWith("91")
                    && normalized.length() == 12) {

                normalized =
                        normalized.substring(2);
            }

            if (!PHONE_PATTERN.matcher(normalized).matches()) {

                throw new ApiException(
                        "INVALID_PHONE",
                        "Invalid Indian phone number",
                        HttpStatus.BAD_REQUEST
                );
            }

            return normalized;
        }

        throw new ApiException(
                "INVALID_DESTINATION_TYPE",
                "Destination type must be PHONE or EMAIL",
                HttpStatus.BAD_REQUEST
        );
    }

    private String normalizeDestinationType(
            String destinationType
    ) {

        if (destinationType == null
                || destinationType.isBlank()) {

            throw new ApiException(
                    "DESTINATION_TYPE_REQUIRED",
                    "Destination type is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        return destinationType
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizePurpose(String purpose) {

        if (purpose == null || purpose.isBlank()) {

            throw new ApiException(
                    "OTP_PURPOSE_REQUIRED",
                    "OTP purpose is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        return purpose
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private void validatePurposeAndDestinationType(
            String purpose,
            String destinationType
    ) {

        switch (purpose) {

            case "PHONE_LOGIN",
                 "PHONE_VERIFICATION" -> {

                if (!"PHONE".equals(destinationType)) {

                    throw new ApiException(
                            "INVALID_OTP_DESTINATION",
                            "This OTP purpose requires a phone number",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }

            case "EMAIL_LOGIN",
                 "EMAIL_VERIFICATION" -> {

                if (!"EMAIL".equals(destinationType)) {

                    throw new ApiException(
                            "INVALID_OTP_DESTINATION",
                            "This OTP purpose requires an email address",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }

            case "PASSWORD_RESET" -> {

                if (!"PHONE".equals(destinationType)
                        && !"EMAIL".equals(destinationType)) {

                    throw new ApiException(
                            "INVALID_OTP_DESTINATION",
                            "Password reset requires a phone number or email",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }

            default -> throw new ApiException(
                    "INVALID_OTP_PURPOSE",
                    "Invalid OTP purpose",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}