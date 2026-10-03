package com.mittiandmore.controller;

import com.mittiandmore.dto.ShippingQuoteRequest;
import com.mittiandmore.dto.ShippingQuoteResponse;
import com.mittiandmore.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> quote(
            @Valid @RequestBody ShippingQuoteRequest request) {
        return ResponseEntity.ok(shippingService.calculateQuote(request));
    }
}
