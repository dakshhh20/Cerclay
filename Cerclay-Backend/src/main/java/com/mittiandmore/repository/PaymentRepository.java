package com.mittiandmore.repository;

import com.mittiandmore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByCashfreeOrderId(String cashfreeOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}