package com.mittiandmore.controller;

import com.mittiandmore.config.DualClientSecurityContextRepository;
import com.mittiandmore.dto.GoogleLoginRequest;
import com.mittiandmore.dto.LoginRequest;
import com.mittiandmore.dto.LoginResponse;
import com.mittiandmore.dto.RegisterRequest;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.CustomerService;
import com.mittiandmore.service.CustomerUserDetailsService;
import com.mittiandmore.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final CustomerUserDetailsService customerUserDetailsService;
    private final GoogleAuthService googleAuthService;

    private final SecurityContextRepository securityContextRepository =
        new DualClientSecurityContextRepository().customerRepository();

    public AuthController(
        @Qualifier("customerAuthenticationManager") AuthenticationManager authenticationManager,
        CustomerRepository customerRepository,
        CustomerService customerService,
        CustomerUserDetailsService customerUserDetailsService,
        GoogleAuthService googleAuthService
    ) {
        this.authenticationManager = authenticationManager;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.customerUserDetailsService = customerUserDetailsService;
        this.googleAuthService = googleAuthService;
    }

    /*
     * Customer Registration
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Customer customer = customerService.registerCustomer(request);

            LoginResponse response = new LoginResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /*
     * Customer Login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            Customer customer = customerRepository.findByEmail(request.getEmail()).orElseThrow();

            LoginResponse response = new LoginResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone()
            );

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Customer account is disabled");
        }
    }

    /*
     * Customer Login with Google
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(
        @Valid @RequestBody GoogleLoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        Customer customer = googleAuthService.authenticate(request.getCredential());

        UserDetails userDetails = customerUserDetailsService.loadUserByUsername(customer.getEmail());

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            userDetails,
            null,
            userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // The Google endpoint is public, so there may be no HTTP session yet.
        // Create one before rotating the session ID; Tomcat cannot rotate an
        // ID when no session exists. This preserves the existing customer/admin
        // session separation while still applying session-fixation protection.
        httpRequest.getSession(true);
        httpRequest.changeSessionId();

        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        LoginResponse response = new LoginResponse(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone()
        );

        return ResponseEntity.ok(response);
    }

    /*
     * Customer Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("CERCLAY_CUSTOMER_SECURITY_CONTEXT");
        }

        return ResponseEntity.ok("Logout successful");
    }

    /*
     * Get Currently Logged-In Customer
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);

        if (customer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Customer not found");
        }

        LoginResponse response = new LoginResponse(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone()
        );

        return ResponseEntity.ok(response);
    }
}
