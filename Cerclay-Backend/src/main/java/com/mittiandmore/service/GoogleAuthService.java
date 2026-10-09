package com.mittiandmore.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CustomerRepository;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.client-id:}")
    private String googleClientId;

    public GoogleAuthService(
        Optional<GoogleIdTokenVerifier> verifier,
        CustomerRepository customerRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.verifier = verifier.orElse(null);
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Customer authenticate(String credential) {
        if (googleClientId == null || googleClientId.isBlank() || verifier == null) {
            throw new ApiException(
                "GOOGLE_LOGIN_NOT_CONFIGURED",
                "Google Login is not configured yet",
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        if (credential == null || credential.isBlank()) {
            throw new ApiException(
                "GOOGLE_CREDENTIAL_REQUIRED",
                "Google credential is required",
                HttpStatus.BAD_REQUEST
            );
        }

        GoogleIdToken idToken;

        try {
            idToken = verifier.verify(credential);
        } catch (GeneralSecurityException | IOException exception) {
            throw new ApiException(
                "INVALID_GOOGLE_CREDENTIAL",
                "Google credential could not be verified",
                HttpStatus.UNAUTHORIZED
            );
        }

        if (idToken == null) {
            throw new ApiException(
                "INVALID_GOOGLE_CREDENTIAL",
                "Google credential could not be verified",
                HttpStatus.UNAUTHORIZED
            );
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String googleId = payload.getSubject();

        String email = payload.getEmail();

        Boolean emailVerified = payload.getEmailVerified();

        if (
            googleId == null ||
            googleId.isBlank() ||
            email == null ||
            email.isBlank() ||
            !Boolean.TRUE.equals(emailVerified)
        ) {
            throw new ApiException(
                "INVALID_GOOGLE_ACCOUNT",
                "Google account information could not be verified",
                HttpStatus.UNAUTHORIZED
            );
        }

        Customer customer = customerRepository.findByGoogleId(googleId).orElse(null);

        /*
         * Existing customer already linked
         * to this Google account.
         */
        if (customer != null) {
            ensureActive(customer);

            return customer;
        }

        /*
         * Check whether a customer already exists
         * with the same email address.
         */
        customer = customerRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (customer != null) {
            /*
             * Google has verified the email.
             *
             * For Gmail or a verified Google Workspace
             * hosted domain, we can safely associate
             * the Google identity with the existing
             * account.
             */
            if (isGoogleAuthoritativeEmail(payload)) {
                customer.setGoogleId(googleId);
                customer.setEmailVerified(true);

                ensureActive(customer);

                return customerRepository.save(customer);
            }

            /*
             * Do not silently link an untrusted identity
             * to an existing account.
             */
            throw new ApiException(
                "ACCOUNT_ALREADY_EXISTS",
                "An account with this email already exists. Please sign in with your existing account first.",
                HttpStatus.CONFLICT
            );
        }

        /*
         * No existing customer.
         *
         * Create a new customer account.
         */
        Customer newCustomer = new Customer();

        /*
         * Google provides the user's display name
         * as the "name" claim.
         */
        String name = (String) payload.get("name");

        if (name == null || name.isBlank()) {
            int atIndex = email.indexOf('@');

            if (atIndex > 0) {
                name = email.substring(0, atIndex);
            } else {
                name = "Google User";
            }
        }

        newCustomer.setName(name.trim());

        newCustomer.setEmail(email.trim().toLowerCase());

        newCustomer.setGoogleId(googleId);

        /*
         * Google-authenticated customers don't need
         * to know this generated password.
         *
         * It prevents the password field from being null
         * if the database requires a password value.
         */
        newCustomer.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        newCustomer.setActive(true);

        newCustomer.setRole("CUSTOMER");

        newCustomer.setEmailVerified(true);

        newCustomer.setPhoneVerified(false);

        return customerRepository.save(newCustomer);
    }

    private boolean isGoogleAuthoritativeEmail(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();

        String hostedDomain = payload.getHostedDomain();

        return (
            (email != null && email.toLowerCase().endsWith("@gmail.com")) ||
            (hostedDomain != null && !hostedDomain.isBlank())
        );
    }

    private void ensureActive(Customer customer) {
        if (Boolean.FALSE.equals(customer.getActive())) {
            throw new ApiException("CUSTOMER_ACCOUNT_DISABLED", "Customer account is disabled", HttpStatus.FORBIDDEN);
        }
    }
}
