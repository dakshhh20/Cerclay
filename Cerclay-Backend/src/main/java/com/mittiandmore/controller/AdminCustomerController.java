package com.mittiandmore.controller;
import com.mittiandmore.dto.ActiveStatusRequest;
import com.mittiandmore.dto.CustomerResponse;
import com.mittiandmore.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {
    private final CustomerService service;
    public AdminCustomerController(CustomerService service){this.service=service;}
    @GetMapping public ResponseEntity<List<CustomerResponse>> all(){return ResponseEntity.ok(service.getAllCustomerResponses());}
    @PutMapping("/{id}/active") public ResponseEntity<CustomerResponse> active(@PathVariable Long id,@Valid @RequestBody ActiveStatusRequest request){return ResponseEntity.ok(service.setActive(id,Boolean.TRUE.equals(request.getActive())));}
}
