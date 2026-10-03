package com.mittiandmore.controller;

import com.mittiandmore.dto.CustomerResponse;
import com.mittiandmore.dto.CustomerUpdateRequest;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.service.CustomerService;
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

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerUserDetailsService customerUserDetailsService;

    private final SecurityContextRepository securityContextRepository =
            new DualClientSecurityContextRepository().customerRepository();

    public CustomerController(
            CustomerService customerService,
            CustomerUserDetailsService customerUserDetailsService) {

        this.customerService = customerService;
        this.customerUserDetailsService =
                customerUserDetailsService;
    }

    // ADMIN ONLY
    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomerResponses();
    }

    // CUSTOMER: get own profile
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(
            @PathVariable Long id,
            Authentication authentication) {

        Customer customer =
                customerService.getCustomerById(id);

        if (customer == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
        }

        if (!customer.getEmail()
                .equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to access this customer");
        }

        return ResponseEntity.ok(
                customerService.toResponse(customer)
        );
    }

    // Direct customer creation is disabled.
    // Customers must register through /api/auth/register.

    // CUSTOMER: update own profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        Customer existingCustomer =
                customerService.getCustomerById(id);

        if (existingCustomer == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
        }

        if (!existingCustomer.getEmail()
                .equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to modify this customer");
        }

        boolean emailChanged =
                !Objects.equals(
                        existingCustomer.getEmail(),
                        request.getEmail()
                );

        Customer updatedCustomer =
                customerService.updateCustomer(id, request);

        /*
         * If the email changed, the authenticated username
         * must also be updated because Spring Security uses
         * the customer's email as the session username.
         */
        if (emailChanged) {

            UserDetails userDetails =
                    customerUserDetailsService.loadUserByUsername(
                            updatedCustomer.getEmail()
                    );

            Authentication newAuthentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(
                    newAuthentication
            );

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );
        }

        return ResponseEntity.ok(
                customerService.toResponse(updatedCustomer)
        );
    }
}