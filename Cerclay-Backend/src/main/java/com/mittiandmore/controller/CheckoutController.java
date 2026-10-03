package com.mittiandmore.controller;

import com.mittiandmore.dto.CheckoutSummaryRequest;
import com.mittiandmore.dto.CheckoutSummaryResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;
    private final CustomerRepository customerRepository;
    public CheckoutController(CheckoutService checkoutService, CustomerRepository customerRepository) { this.checkoutService = checkoutService; this.customerRepository = customerRepository; }

    @PostMapping("/summary")
    public ResponseEntity<CheckoutSummaryResponse> summary(@Valid @RequestBody CheckoutSummaryRequest request, Authentication authentication) {
        Customer customer = customerRepository.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
        return ResponseEntity.ok(checkoutService.summary(customer.getId(), request));
    }
}
