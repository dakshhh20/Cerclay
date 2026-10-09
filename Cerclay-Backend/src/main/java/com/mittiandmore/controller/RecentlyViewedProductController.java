package com.mittiandmore.controller;

import com.mittiandmore.dto.ProductResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.RecentlyViewedProductService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recently-viewed")
public class RecentlyViewedProductController {

    private final RecentlyViewedProductService recentlyViewedService;
    private final CustomerRepository customerRepository;

    public RecentlyViewedProductController(
        RecentlyViewedProductService recentlyViewedService,
        CustomerRepository customerRepository
    ) {
        this.recentlyViewedService = recentlyViewedService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getRecentlyViewed(Authentication authentication) {
        Customer customer = authenticatedCustomer(authentication);
        return ResponseEntity.ok(recentlyViewedService.getRecentlyViewed(customer.getId()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<List<ProductResponse>> recordRecentlyViewed(
        @PathVariable Long productId,
        Authentication authentication
    ) {
        Customer customer = authenticatedCustomer(authentication);
        return ResponseEntity.ok(recentlyViewedService.recordView(customer.getId(), productId));
    }

    private Customer authenticatedCustomer(Authentication authentication) {
        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getName())
        ) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication required"
            );
        }

        return customerRepository
            .findByEmail(authentication.getName())
            .orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer account required"
                )
            );
    }
}
