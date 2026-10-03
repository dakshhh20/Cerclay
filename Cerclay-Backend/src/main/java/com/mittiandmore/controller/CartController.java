package com.mittiandmore.controller;

import com.mittiandmore.dto.CartResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CustomerRepository customerRepository;

    public CartController(
            CartService cartService,
            CustomerRepository customerRepository) {

        this.cartService = cartService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public CartResponse getCart(
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            Authentication authentication) {

        if (isCustomerLoggedIn(authentication)) {

            Long customerId =
                    getAuthenticatedCustomerId(authentication);

            return cartService.getOrCreateCustomerCart(
                    customerId
            );
        }

        return cartService.getOrCreateGuestCart(guestId);
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "1") int packSize,
            Authentication authentication) {

        if (isCustomerLoggedIn(authentication)) {

            Long customerId =
                    getAuthenticatedCustomerId(authentication);

            return cartService.addItemToCustomerCart(
                    customerId,
                    productId,
                    quantity,
                    packSize
            );
        }

        return cartService.addItem(
                guestId,
                productId,
                quantity,
                packSize
        );
    }

    @PutMapping("/items")
    public CartResponse updateItemQuantity(
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "1") int packSize,
            Authentication authentication) {

        if (isCustomerLoggedIn(authentication)) {

            Long customerId =
                    getAuthenticatedCustomerId(authentication);

            return cartService.updateCustomerCartItemQuantity(
                    customerId,
                    productId,
                    quantity,
                    packSize
            );
        }

        return cartService.updateItemQuantity(
                guestId,
                productId,
                quantity,
                packSize
        );
    }

    @DeleteMapping("/items")
    public CartResponse removeItem(
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int packSize,
            Authentication authentication) {

        if (isCustomerLoggedIn(authentication)) {

            Long customerId =
                    getAuthenticatedCustomerId(authentication);

            return cartService.removeCustomerCartItem(
                    customerId,
                    productId,
                    packSize
            );
        }

        return cartService.removeItem(
                guestId,
                productId,
                packSize
        );
    }

    @PostMapping("/merge-guest")
    public CartResponse mergeGuestCart(
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            Authentication authentication) {

        if (!isCustomerLoggedIn(authentication)) {
            return cartService.getOrCreateGuestCart(guestId);
        }

        Long customerId = getAuthenticatedCustomerId(authentication);
        return cartService.mergeGuestCart(customerId, guestId);
    }

    /*
     * Only a real customer account should switch the cart to the
     * customer-owned cart. An authenticated admin session (which can
     * share localhost cookies with the customer app) must still use
     * the guest cart rather than being treated as a customer.
     */
    private boolean isCustomerLoggedIn(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return false;
        }

        return customerRepository
                .findByEmail(authentication.getName())
                .isPresent();
    }

    private Long getAuthenticatedCustomerId(
            Authentication authentication) {

        String email = authentication.getName();

        Customer customer =
                customerRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Authenticated customer not found"
                                )
                        );

        return customer.getId();
    }
}