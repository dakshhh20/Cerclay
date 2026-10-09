package com.mittiandmore.repository;

import com.mittiandmore.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByCashfreeOrderId(String cashfreeOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
