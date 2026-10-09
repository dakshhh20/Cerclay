package com.mittiandmore.controller;

import com.mittiandmore.dto.ProductResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.WishlistService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final CustomerRepository customerRepository;

    public WishlistController(WishlistService wishlistService, CustomerRepository customerRepository) {
        this.wishlistService = wishlistService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getWishlist(Authentication authentication) {
        Customer customer = authenticatedCustomer(authentication);
        return ResponseEntity.ok(wishlistService.getWishlist(customer.getId()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<List<ProductResponse>> addToWishlist(
        @PathVariable Long productId,
        Authentication authentication
    ) {
        Customer customer = authenticatedCustomer(authentication);
        return ResponseEntity.ok(wishlistService.add(customer.getId(), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<List<ProductResponse>> removeFromWishlist(
        @PathVariable Long productId,
        Authentication authentication
    ) {
        Customer customer = authenticatedCustomer(authentication);
        return ResponseEntity.ok(wishlistService.remove(customer.getId(), productId));
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
