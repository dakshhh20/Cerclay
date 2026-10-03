package com.mittiandmore.controller;

import com.mittiandmore.dto.DiscountValidationResponse;
import com.mittiandmore.dto.PublicDiscountResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.DiscountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {
    private final DiscountService discountService;
    private final CustomerRepository customerRepository;

    public DiscountController(DiscountService discountService, CustomerRepository customerRepository) {
        this.discountService = discountService;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/available")
    public ResponseEntity<List<PublicDiscountResponse>> available(Authentication authentication) {
        Long customerId = null;
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            customerId = customerRepository.findByEmail(authentication.getName()).map(Customer::getId).orElse(null);
        }
        return ResponseEntity.ok(discountService.available(customerId));
    }

    @GetMapping("/validate")
    public ResponseEntity<DiscountValidationResponse> validate(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal,
            Authentication authentication) {
        Long customerId = null;
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            customerId = customerRepository.findByEmail(authentication.getName()).map(Customer::getId).orElse(null);
        }
        BigDecimal amount = discountService.calculate(code, subtotal, customerId);
        return ResponseEntity.ok(new DiscountValidationResponse(
                code.trim().toUpperCase(), amount, subtotal.setScale(2, java.math.RoundingMode.HALF_UP)
        ));
    }
}
