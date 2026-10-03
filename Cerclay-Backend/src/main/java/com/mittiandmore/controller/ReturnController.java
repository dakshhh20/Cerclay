package com.mittiandmore.controller;

import com.mittiandmore.dto.CreateReturnRequest;
import com.mittiandmore.dto.ReturnResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {
    private final ReturnService service; private final CustomerRepository customers;
    public ReturnController(ReturnService service, CustomerRepository customers){this.service=service;this.customers=customers;}
    @PostMapping("/orders/{orderId}") public ResponseEntity<ReturnResponse> create(@PathVariable Long orderId,@Valid @RequestBody CreateReturnRequest request,Authentication auth){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(customerId(auth),orderId,request));}
    @PostMapping("/{id}/photos") public ResponseEntity<ReturnResponse> photos(@PathVariable Long id,@RequestParam("files") List<MultipartFile> files,Authentication auth){return ResponseEntity.ok(service.addPhotos(customerId(auth),id,files));}
    @GetMapping public ResponseEntity<List<ReturnResponse>> mine(Authentication auth){return ResponseEntity.ok(service.customerReturns(customerId(auth)));}
    @GetMapping("/{id}") public ResponseEntity<ReturnResponse> get(@PathVariable Long id,Authentication auth){return ResponseEntity.ok(service.getCustomerReturn(customerId(auth),id));}
    private Long customerId(Authentication a){return customers.findByEmail(a.getName()).orElseThrow(()->new IllegalStateException("Authenticated customer not found")).getId();}
}
