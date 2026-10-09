package com.mittiandmore.controller;

import com.mittiandmore.dto.CashfreeOrderResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.CashfreePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/cashfree")
public class CashfreePaymentController {

    private final CashfreePaymentService service;
    private final CustomerRepository customerRepository;

    public CashfreePaymentController(CashfreePaymentService service, CustomerRepository customerRepository) {
        this.service = service;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/orders/{orderId}")
    public ResponseEntity<CashfreeOrderResponse> createOrder(
        @PathVariable Long orderId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(service.createGatewayOrder(customerId(authentication), orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<CashfreeOrderResponse> verify(@RequestParam String orderId, Authentication authentication) {
        return ResponseEntity.ok(service.verifyPayment(customerId(authentication), orderId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
        @RequestHeader(value = "x-webhook-signature", required = false) String signature,
        @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp,
        @RequestHeader(value = "x-webhook-event-id", required = false) String eventId,
        @RequestBody String payload
    ) {
        service.handleWebhook(payload, signature, timestamp, eventId);
        return ResponseEntity.ok().build();
    }

    private Long customerId(Authentication authentication) {
        Customer customer = customerRepository
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
        return customer.getId();
    }
}
