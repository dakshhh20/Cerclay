package com.mittiandmore.controller;

import com.mittiandmore.dto.ProductReviewRequest;
import com.mittiandmore.dto.ProductReviewResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ProductReviewController {
    private final ProductReviewService service; private final CustomerRepository customers;
    public ProductReviewController(ProductReviewService s, CustomerRepository c){service=s;customers=c;}
    @GetMapping("/product/{productId}") public List<ProductReviewResponse> approved(@PathVariable Long productId){return service.approved(productId);}
    @GetMapping("/product/{productId}/eligibility") public ResponseEntity<Boolean> eligibility(@PathVariable Long productId, Authentication auth){return ResponseEntity.ok(service.eligible(customerId(auth),productId));}
    @PostMapping("/product/{productId}") public ResponseEntity<ProductReviewResponse> create(@PathVariable Long productId,@Valid @RequestBody ProductReviewRequest request,Authentication auth){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(customerId(auth),productId,request));}
    @GetMapping("/featured") public List<ProductReviewResponse> featured(){return service.featured();}
    @GetMapping("/mine") public List<ProductReviewResponse> mine(Authentication auth){return service.mine(customerId(auth));}
    @GetMapping("/admin") public List<ProductReviewResponse> all(){return service.all();}
    @PutMapping("/admin/{id}/status") public ProductReviewResponse moderate(@PathVariable Long id,@RequestParam String status){return service.moderate(id,status);}
    @DeleteMapping("/admin/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){service.delete(id);return ResponseEntity.noContent().build();}
    private Long customerId(Authentication a){Customer c=customers.findByEmail(a.getName()).orElseThrow(()->new IllegalStateException("Authenticated customer not found"));return c.getId();}
}
