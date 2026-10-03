package com.mittiandmore.controller;

import com.mittiandmore.dto.RazorpayOrderResponse;
import com.mittiandmore.dto.RazorpayVerifyRequest;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayPaymentController {
    private final RazorpayPaymentService service;
    private final CustomerRepository customerRepository;

    public RazorpayPaymentController(RazorpayPaymentService service, CustomerRepository customerRepository) {
        this.service = service;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/orders/{orderId}")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@PathVariable Long orderId, Authentication authentication) {
        return ResponseEntity.ok(service.createGatewayOrder(customerId(authentication), orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@Valid @RequestBody RazorpayVerifyRequest request, Authentication authentication) {
        service.verifyPayment(customerId(authentication), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                        @RequestHeader(value = "x-razorpay-event-id", required = false) String eventId,
                                        @RequestBody String payload) {
        service.handleWebhook(payload, signature, eventId);
        return ResponseEntity.ok().build();
    }

    private Long customerId(Authentication authentication) {
        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
        return customer.getId();
    }
}
