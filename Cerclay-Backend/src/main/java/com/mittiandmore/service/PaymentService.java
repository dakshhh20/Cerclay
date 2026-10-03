package com.mittiandmore.service;

import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.Payment;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Payment createPaymentRecord(
            Long orderId,
            String razorpayOrderId
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found"
                        )
                );

        if (!"RAZORPAY".equalsIgnoreCase(
                order.getPaymentMethod()
        )) {

            throw new IllegalStateException(
                    "Payment record can only be created for Razorpay orders"
            );
        }

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException(
                    "Payment already exists for this order"
            );
        }

        if (razorpayOrderId == null
                || razorpayOrderId.isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay order ID is required"
            );
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmount(order.getTotal());
        payment.setCurrency("INR");
        payment.setPaymentStatus("CREATED");
        payment.setPaymentMethod("RAZORPAY");

        return paymentRepository.save(payment);
    }
}