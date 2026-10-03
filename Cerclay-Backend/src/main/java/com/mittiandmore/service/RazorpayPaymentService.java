package com.mittiandmore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mittiandmore.config.RazorpayProperties;
import com.mittiandmore.dto.RazorpayOrderResponse;
import com.mittiandmore.dto.RazorpayVerifyRequest;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.Payment;
import com.mittiandmore.entity.PaymentWebhookEvent;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.PaymentRepository;
import com.mittiandmore.repository.PaymentWebhookEventRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class RazorpayPaymentService {
    private final RazorpayProperties properties;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final RefundService refundService;

    public RazorpayPaymentService(RazorpayProperties properties, OrderRepository orderRepository,
                                  CustomerRepository customerRepository, PaymentRepository paymentRepository,
                                  PaymentWebhookEventRepository webhookEventRepository, ObjectMapper objectMapper, RefundService refundService) {
        this.properties = properties;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
        this.refundService = refundService;
    }

    @Transactional
    public RazorpayOrderResponse createGatewayOrder(Long customerId, Long orderId) {
        requireConfigured();
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        if (!order.getCustomer().getId().equals(customer.getId())) throw new ApiException("ORDER_ACCESS_DENIED", "You are not allowed to pay for this order", HttpStatus.FORBIDDEN);
        if (!"RAZORPAY".equalsIgnoreCase(order.getPaymentMethod())) throw new ApiException("INVALID_PAYMENT_METHOD", "This order is not a Razorpay order", HttpStatus.BAD_REQUEST);
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) throw new ApiException("ORDER_ALREADY_PAID", "Order is already paid", HttpStatus.CONFLICT);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null && payment.getRazorpayOrderId() != null && !payment.getRazorpayOrderId().isBlank()) {
            return response(order, payment);
        }

        try {
            RazorpayClient client = new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
            JSONObject options = new JSONObject();
            options.put("amount", toPaise(order.getTotal()));
            options.put("currency", "INR");
            options.put("receipt", order.getOrderNumber());
            options.put("notes", new JSONObject().put("cerclay_order_id", order.getId().toString()));
            if (properties.getConfigurationId() != null && !properties.getConfigurationId().isBlank()) {
                options.put("config_id", properties.getConfigurationId().trim());
            }
            com.razorpay.Order gatewayOrder = client.orders.create(options);
            String gatewayOrderId = gatewayOrder.get("id").toString();
            Payment record = payment == null ? new Payment() : payment;
            record.setOrder(order);
            record.setRazorpayOrderId(gatewayOrderId);
            record.setAmount(order.getTotal());
            record.setCurrency("INR");
            record.setPaymentStatus("CREATED");
            record.setPaymentMethod("RAZORPAY");
            paymentRepository.save(record);
            return response(order, record);
        } catch (Exception e) {
            throw new ApiException("RAZORPAY_ORDER_CREATION_FAILED", "Unable to create the Razorpay payment order", HttpStatus.BAD_GATEWAY);
        }
    }

    @Transactional
    public void verifyPayment(Long customerId, RazorpayVerifyRequest request) {
        requireConfigured();
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "Payment record not found", HttpStatus.NOT_FOUND));
        Order order = payment.getOrder();
        if (!order.getCustomer().getId().equals(customerId)) throw new ApiException("PAYMENT_ACCESS_DENIED", "You are not allowed to verify this payment", HttpStatus.FORBIDDEN);
        if ("PAID".equalsIgnoreCase(payment.getPaymentStatus())) return;
        if (!payment.getRazorpayOrderId().equals(request.getRazorpayOrderId())) throw new ApiException("PAYMENT_ORDER_MISMATCH", "Razorpay order mismatch", HttpStatus.BAD_REQUEST);
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());
            Utils.verifyPaymentSignature(attributes, properties.getKeySecret());
            RazorpayClient client = new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
            com.razorpay.Payment gatewayPayment = client.payments.fetch(request.getRazorpayPaymentId());
            String status = String.valueOf(gatewayPayment.get("status"));
            long amountPaise = Long.parseLong(String.valueOf(gatewayPayment.get("amount")));
            long expectedPaise = toPaise(order.getTotal());
            if (!"captured".equalsIgnoreCase(status) || amountPaise != expectedPaise) {
                throw new ApiException("PAYMENT_NOT_CAPTURED", "Razorpay payment is not captured for the expected amount", HttpStatus.BAD_REQUEST);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("INVALID_PAYMENT_SIGNATURE", "Payment verification failed", HttpStatus.BAD_REQUEST);
        }
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setPaymentStatus("PAID");
        order.setPaymentStatus("PAID");
        paymentRepository.save(payment);
        orderRepository.save(order);
    }

    @Transactional
    public void handleWebhook(String payload, String signature, String eventId) {
        requireWebhookConfigured();
        try {
            Utils.verifyWebhookSignature(payload, signature, properties.getWebhookSecret());
        } catch (Exception e) {
            throw new ApiException("INVALID_WEBHOOK_SIGNATURE", "Invalid Razorpay webhook signature", HttpStatus.UNAUTHORIZED);
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("event").asText("unknown");
            String stableEventId = (eventId == null || eventId.isBlank()) ? sha256(payload + signature) : eventId;
            if (webhookEventRepository.findByEventId(stableEventId).isPresent()) return;
            PaymentWebhookEvent event = new PaymentWebhookEvent();
            event.setEventId(stableEventId); event.setEventType(eventType);
            webhookEventRepository.save(event);

            JsonNode refundEntity = root.path("payload").path("refund").path("entity");
            if (eventType.startsWith("refund.") && !refundEntity.isMissingNode()) {
                refundService.handleWebhook(eventType, new JSONObject(refundEntity.toString()));
            }

            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String razorpayPaymentId = paymentEntity.path("id").asText(null);
            String razorpayOrderId = paymentEntity.path("order_id").asText(null);
            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                razorpayOrderId = root.path("payload").path("order").path("entity").path("id").asText(null);
            }
            Payment payment = razorpayOrderId == null ? null : paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
            if (payment != null && ("payment.captured".equals(eventType) || "order.paid".equals(eventType))) {
                BigDecimal expected = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
                long webhookAmountPaise = paymentEntity.path("amount").asLong(-1L);
                if (webhookAmountPaise >= 0 && expected.multiply(BigDecimal.valueOf(100)).longValueExact() != webhookAmountPaise) {
                    throw new ApiException("PAYMENT_AMOUNT_MISMATCH", "Razorpay webhook amount does not match the order", HttpStatus.BAD_REQUEST);
                }
                if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setPaymentStatus("PAID");
                payment.getOrder().setPaymentStatus("PAID");
                paymentRepository.save(payment);
                orderRepository.save(payment.getOrder());
            } else if (payment != null && ("payment.failed".equals(eventType))) {
                payment.setPaymentStatus("FAILED");
                paymentRepository.save(payment);
            }
            event.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(event);
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException("WEBHOOK_PROCESSING_FAILED", "Unable to process Razorpay webhook", HttpStatus.BAD_REQUEST); }
    }

    private RazorpayOrderResponse response(Order order, Payment payment) {
        RazorpayOrderResponse r = new RazorpayOrderResponse();
        r.setOrderId(order.getId()); r.setOrderNumber(order.getOrderNumber()); r.setRazorpayOrderId(payment.getRazorpayOrderId());
        r.setAmount(payment.getAmount()); r.setCurrency(payment.getCurrency()); r.setKeyId(properties.getKeyId()); r.setPaymentStatus(payment.getPaymentStatus());
        return r;
    }

    private long toPaise(BigDecimal amount) { return amount.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).longValueExact(); }
    private void requireConfigured() { if (!properties.isEnabled() || blank(properties.getKeyId()) || blank(properties.getKeySecret())) throw new ApiException("RAZORPAY_NOT_CONFIGURED", "Razorpay is not configured", HttpStatus.SERVICE_UNAVAILABLE); }
    private void requireWebhookConfigured() { if (!properties.isEnabled() || blank(properties.getWebhookSecret())) throw new ApiException("RAZORPAY_WEBHOOK_NOT_CONFIGURED", "Razorpay webhook is not configured", HttpStatus.SERVICE_UNAVAILABLE); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String sha256(String input) throws Exception { byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format("%02x", b)); return out.toString(); }
}
