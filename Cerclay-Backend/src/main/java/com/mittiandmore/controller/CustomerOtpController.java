package com.mittiandmore.controller;

import com.mittiandmore.dto.PasswordResetRequest;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.CustomerOtpVerification;
import com.mittiandmore.service.CustomerOtpService;
import com.mittiandmore.service.CustomerUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.mittiandmore.config.DualClientSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/otp")
public class CustomerOtpController {

    private final CustomerOtpService customerOtpService;
    private final CustomerUserDetailsService customerUserDetailsService;

    private final SecurityContextRepository securityContextRepository =
            new DualClientSecurityContextRepository().customerRepository();

    public CustomerOtpController(
            CustomerOtpService customerOtpService,
            CustomerUserDetailsService customerUserDetailsService
    ) {
        this.customerOtpService = customerOtpService;
        this.customerUserDetailsService =
                customerUserDetailsService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(
            @RequestParam String destination,
            @RequestParam String destinationType,
            @RequestParam String purpose,
            Authentication authentication
    ) {

        String normalizedPurpose =
                purpose.trim().toUpperCase();

        /*
         * PHONE_LOGIN is public because the customer is not logged in yet.
         * Email OTP login has been removed from the customer authentication flow.
         */
        if ("PHONE_LOGIN".equals(normalizedPurpose)) {

            customerOtpService.sendOtp(
                    destination,
                    destinationType,
                    purpose
            );

            return ResponseEntity.ok(
                    "OTP sent successfully"
            );
        }

        if ("EMAIL_LOGIN".equals(normalizedPurpose)) {
            return ResponseEntity.badRequest()
                    .body("Email OTP login is disabled");
        }

        /*
         * PASSWORD_RESET is also public because the customer
         * is explicitly trying to recover an account.
         */
        if ("PASSWORD_RESET".equals(normalizedPurpose)) {

            customerOtpService.sendOtp(
                    destination,
                    destinationType,
                    purpose
            );

            /*
             * Always return the same response regardless of
             * whether the account exists.
             */
            return ResponseEntity.ok(
                    "If an account exists, an OTP has been sent"
            );
        }

        /*
         * All remaining OTP purposes require authentication.
         */
        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
        }

        Customer customer =
                customerOtpService.getCustomerByEmail(
                        authentication.getName()
                );

        if (customer == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer account not found");
        }

        String verifiedDestination;

        if ("PHONE_VERIFICATION".equals(normalizedPurpose)) {

            verifiedDestination =
                    customer.getPhone();

            if (verifiedDestination == null
                    || verifiedDestination.isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body("Customer phone number is not available");
            }

            destinationType = "PHONE";

        } else if ("EMAIL_VERIFICATION".equals(normalizedPurpose)) {

            verifiedDestination =
                    customer.getEmail();

            if (verifiedDestination == null
                    || verifiedDestination.isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body("Customer email address is not available");
            }

            destinationType = "EMAIL";

        } else {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid OTP purpose");
        }

        customerOtpService.sendOtp(
                verifiedDestination,
                destinationType,
                purpose
        );

        return ResponseEntity.ok(
                "OTP sent successfully"
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyOtp(
            @RequestParam String destination,
            @RequestParam String destinationType,
            @RequestParam String purpose,
            @RequestParam String otp,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        String normalizedPurpose =
                purpose.trim().toUpperCase();

        /*
         * PHONE_LOGIN is public and establishes the customer session.
         * Email OTP login has been removed from the customer authentication flow.
         */
        if ("PHONE_LOGIN".equals(normalizedPurpose)) {

            Customer customer =
                    customerOtpService.verifyPhoneLoginOtp(
                            destination,
                            otp,
                            name,
                            email
                    );

            if (customer == null) {

                return ResponseEntity
                        .badRequest()
                        .body("Customer account not found");
            }

            UserDetails userDetails =
                    customerUserDetailsService.loadUserByUsername(
                            customer.getEmail()
                    );

            Authentication customerAuthentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(
                    customerAuthentication
            );

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            return ResponseEntity.ok(
                    "OTP verified successfully"
            );
        }

        if ("EMAIL_LOGIN".equals(normalizedPurpose)) {
            return ResponseEntity.badRequest()
                    .body("Email OTP login is disabled");
        }

        /*
         * PASSWORD_RESET verification is handled by the
         * password-reset endpoint because it must also change
         * the password.
         */
        if ("PASSWORD_RESET".equals(normalizedPurpose)) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Use the password reset endpoint to verify OTP"
                    );
        }

        /*
         * Existing customer's email/phone verification requires
         * authentication.
         */
        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
        }

        Customer customer =
                customerOtpService.getCustomerByEmail(
                        authentication.getName()
                );

        if (customer == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer account not found");
        }

        String verifiedDestination;

        if ("PHONE_VERIFICATION".equals(normalizedPurpose)) {

            verifiedDestination =
                    customer.getPhone();

            destinationType = "PHONE";

        } else if ("EMAIL_VERIFICATION".equals(normalizedPurpose)) {

            verifiedDestination =
                    customer.getEmail();

            destinationType = "EMAIL";

        } else {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid OTP purpose");
        }

        customerOtpService.verifyOtp(
                verifiedDestination,
                destinationType,
                purpose,
                otp
        );

        return ResponseEntity.ok(
                "OTP verified successfully"
        );
    }

    /*
     * PASSWORD RESET
     *
     * Public endpoint.
     *
     * The OTP and new password are processed together so that
     * a successfully verified OTP immediately performs the
     * password change.
     */
    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        customerOtpService.resetPassword(
                request.getDestination(),
                request.getDestinationType(),
                request.getOtp(),
                request.getNewPassword()
        );

        /*
         * If the customer happens to have an active session,
         * invalidate it after the password change.
         *
         * This prevents an old authenticated session from
         * remaining active after a password reset.
         */
        SecurityContextHolder.clearContext();

        var session =
                httpRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        httpResponse.setStatus(
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(
                "Password reset successfully"
        );
    }
}