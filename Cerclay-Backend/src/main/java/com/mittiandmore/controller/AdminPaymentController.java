package com.mittiandmore.controller;
import com.mittiandmore.dto.AdminPaymentResponse;
import com.mittiandmore.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {
 private final PaymentRepository repository;
 public AdminPaymentController(PaymentRepository repository){this.repository=repository;}
 @GetMapping public ResponseEntity<List<AdminPaymentResponse>> all(){return ResponseEntity.ok(repository.findAllByOrderByCreatedAtDesc().stream().map(AdminPaymentResponse::from).toList());}
}
