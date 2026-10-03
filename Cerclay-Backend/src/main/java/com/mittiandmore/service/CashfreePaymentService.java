package com.mittiandmore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mittiandmore.dto.CashfreeOrderResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.Payment;
import com.mittiandmore.entity.PaymentWebhookEvent;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.PaymentRepository;
import com.mittiandmore.repository.PaymentWebhookEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class CashfreePaymentService {
    private final CashfreeGatewayService gateway;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final RefundService refundService;

    public CashfreePaymentService(CashfreeGatewayService gateway, CustomerRepository customerRepository,
                                  OrderRepository orderRepository, PaymentRepository paymentRepository,
                                  PaymentWebhookEventRepository webhookEventRepository, RefundService refundService) {
        this.gateway = gateway;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.refundService = refundService;
    }

    @Transactional
    public CashfreeOrderResponse createGatewayOrder(Long customerId, Long orderId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        if (!order.getCustomer().getId().equals(customer.getId())) throw new ApiException("ORDER_ACCESS_DENIED", "You are not allowed to pay for this order", HttpStatus.FORBIDDEN);
        if (!"CASHFREE".equalsIgnoreCase(order.getPaymentMethod())) throw new ApiException("INVALID_PAYMENT_METHOD", "This order is not a Cashfree order", HttpStatus.BAD_REQUEST);
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) throw new ApiException("ORDER_ALREADY_PAID", "Order is already paid", HttpStatus.CONFLICT);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null && payment.getCashfreeOrderId() != null && !payment.getCashfreeOrderId().isBlank()
                && payment.getCashfreePaymentSessionId() != null && !payment.getCashfreePaymentSessionId().isBlank()) {
            return response(order, payment);
        }

        String gatewayOrderId = "cerclay_" + order.getId() + "_" + System.currentTimeMillis();
        String returnUrl = gateway.getProperties().getReturnUrl();
        JsonNode result = gateway.createOrder(
                gatewayOrderId,
                String.valueOf(customer.getId()),
                customer.getName(),
                customer.getEmail(),
                (customer.getPhone() == null || customer.getPhone().isBlank()) ? order.getAddressPhone() : customer.getPhone(),
                order.getTotal(),
                returnUrl
        );

        Payment record = payment == null ? new Payment() : payment;
        record.setOrder(order);
        record.setCashfreeOrderId(result.path("order_id").asText(gatewayOrderId));
        record.setCashfreePaymentSessionId(result.path("payment_session_id").asText(null));
        record.setAmount(order.getTotal());
        record.setCurrency("INR");
        record.setPaymentStatus("CREATED");
        record.setPaymentMethod("CASHFREE");
        paymentRepository.save(record);
        return response(order, record);
    }

    @Transactional
    public CashfreeOrderResponse verifyPayment(Long customerId, String cashfreeOrderId) {
        Payment payment = paymentRepository.findByCashfreeOrderId(cashfreeOrderId)
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "Cashfree payment record not found", HttpStatus.NOT_FOUND));
        Order order = payment.getOrder();
        if (!order.getCustomer().getId().equals(customerId)) throw new ApiException("PAYMENT_ACCESS_DENIED", "You are not allowed to verify this payment", HttpStatus.FORBIDDEN);
        if ("PAID".equalsIgnoreCase(payment.getPaymentStatus())) return response(order, payment);

        JsonNode payments = gateway.getPayments(cashfreeOrderId);
        JsonNode success = null;
        if (payments.isArray()) {
            for (JsonNode p : payments) {
                if ("SUCCESS".equalsIgnoreCase(p.path("payment_status").asText())) { success = p; break; }
            }
        }
        if (success == null) {
            throw new ApiException("PAYMENT_NOT_COMPLETED", "Cashfree payment has not completed successfully", HttpStatus.BAD_REQUEST);
        }
        BigDecimal amount = success.path("payment_amount").decimalValue().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(order.getTotal().setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new ApiException("PAYMENT_AMOUNT_MISMATCH", "Cashfree payment amount does not match the order", HttpStatus.BAD_REQUEST);
        }
        markPaid(payment, success);
        return response(order, payment);
    }

    @Transactional
    public void handleWebhook(String payload, String signature, String timestamp, String eventId) {
        gateway.verifyWebhook(timestamp, signature, payload);
        try {
            JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            String eventType = root.path("type").asText(root.path("event").asText("unknown"));
            String stableEventId = eventId;
            if (stableEventId == null || stableEventId.isBlank()) stableEventId = timestamp + ":" + Integer.toHexString(payload.hashCode());
            if (webhookEventRepository.findByEventId(stableEventId).isPresent()) return;
            PaymentWebhookEvent event = new PaymentWebhookEvent();
            event.setEventId(stableEventId);
            event.setEventType(eventType);
            webhookEventRepository.save(event);

            JsonNode data = root.path("data");
            JsonNode refundNode = data.path("refund");
            if (!refundNode.isMissingNode()) {
                refundService.handleWebhook(eventType, new org.json.JSONObject(refundNode.toString()));
            }
            String orderId = data.path("order").path("order_id").asText(null);
            String status = data.path("payment").path("payment_status").asText("");
            JsonNode paymentNode = data.path("payment");
            if (orderId != null && !orderId.isBlank() && "SUCCESS".equalsIgnoreCase(status)) {
                Payment payment = paymentRepository.findByCashfreeOrderId(orderId).orElse(null);
                if (payment != null) markPaid(payment, paymentNode);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("WEBHOOK_PROCESSING_FAILED", "Unable to process Cashfree webhook", HttpStatus.BAD_REQUEST);
        }
    }

    private void markPaid(Payment payment, JsonNode paymentNode) {
        payment.setCashfreePaymentId(paymentNode.path("cf_payment_id").asText(null));
        payment.setPaymentStatus("PAID");
        payment.getOrder().setPaymentStatus("PAID");
        paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());
    }

    private CashfreeOrderResponse response(Order order, Payment payment) {
        CashfreeOrderResponse response = new CashfreeOrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCashfreeOrderId(payment.getCashfreeOrderId());
        response.setPaymentSessionId(payment.getCashfreePaymentSessionId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentStatus(payment.getPaymentStatus());
        response.setEnvironment(gateway.getProperties().getBaseUrl().contains("sandbox") ? "sandbox" : "production");
        return response;
    }
}
