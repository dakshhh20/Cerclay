package com.mittiandmore.service;

import com.mittiandmore.dto.RefundRequest;
import com.mittiandmore.dto.RefundResponse;
import com.mittiandmore.entity.*;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.notification.NotificationEventType;
import com.mittiandmore.notification.NotificationService;
import com.mittiandmore.repository.*;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefundService {
    private final RefundRepository refundRepository;
    private final ReturnRequestRepository returnRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final com.mittiandmore.config.RazorpayProperties razorpayProperties;
    private final CashfreeGatewayService cashfreeGateway;

    public RefundService(RefundRepository refundRepository, ReturnRequestRepository returnRepository,
                         PaymentRepository paymentRepository, OrderRepository orderRepository,
                         NotificationService notificationService, com.mittiandmore.config.RazorpayProperties razorpayProperties,
                         CashfreeGatewayService cashfreeGateway) {
        this.refundRepository=refundRepository; this.returnRepository=returnRepository; this.paymentRepository=paymentRepository;
        this.orderRepository=orderRepository; this.notificationService=notificationService; this.razorpayProperties=razorpayProperties; this.cashfreeGateway=cashfreeGateway;
    }

    @Transactional
    public RefundResponse create(Long returnId, RefundRequest request) {
        ReturnRequest rr=returnRepository.findByIdForUpdate(returnId).orElseThrow(()->new ApiException("RETURN_NOT_FOUND","Return request not found",HttpStatus.NOT_FOUND));
        if(!"RECEIVED".equals(rr.getStatus()) && !"REFUND_PENDING".equals(rr.getStatus())) throw new ApiException("REFUND_NOT_ALLOWED","Return must be received before refund can be initiated",HttpStatus.CONFLICT);
        BigDecimal amount=money(request.getAmount());
        BigDecimal already=money(refundRepository.sumSuccessfulOrPendingByReturnId(returnId));
        BigDecimal selectedItemsRefund = BigDecimal.ZERO;
        for (ReturnItem item : rr.getItems()) {
            selectedItemsRefund = selectedItemsRefund.add(
                    item.getOrderItem().getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }
        BigDecimal returnRemaining = money(selectedItemsRefund.subtract(already).max(BigDecimal.ZERO));
        BigDecimal orderRemaining = money(rr.getOrder().getTotal()
                .subtract(refundRepository.sumSuccessfulOrPendingByOrderId(rr.getOrder().getId()))
                .max(BigDecimal.ZERO));
        BigDecimal remaining = returnRemaining.min(orderRemaining);
        if(amount.compareTo(remaining)>0) throw new ApiException("REFUND_AMOUNT_EXCEEDED","Refund amount exceeds the refundable value of the selected return items",HttpStatus.BAD_REQUEST);
        if(amount.compareTo(BigDecimal.ZERO)<=0) throw new ApiException("INVALID_REFUND_AMOUNT","Refund amount must be positive",HttpStatus.BAD_REQUEST);

        if("COD".equalsIgnoreCase(rr.getOrder().getPaymentMethod())) {
            com.mittiandmore.entity.Refund record=new com.mittiandmore.entity.Refund(); record.setReturnRequest(rr); record.setOrder(rr.getOrder()); record.setAmount(amount); record.setCurrency("INR"); record.setStatus("MANUAL_PENDING"); record.setReason(request.getReason()); record.setReceipt(receipt(rr));
            refundRepository.save(record);
            rr.setRefundRequestedAmount(already.add(amount));
            rr.setStatus("REFUND_PENDING");
            returnRepository.save(rr);
            completeReturnIfSatisfied(record);
            return toResponse(record);
        }

        Payment payment=paymentRepository.findByOrderId(rr.getOrder().getId()).orElseThrow(()->new ApiException("PAYMENT_NOT_FOUND","Payment record not found",HttpStatus.CONFLICT));
        if ("CASHFREE".equalsIgnoreCase(rr.getOrder().getPaymentMethod())) {
            if(!"PAID".equalsIgnoreCase(payment.getPaymentStatus()) || payment.getCashfreeOrderId()==null || payment.getCashfreeOrderId().isBlank()) throw new ApiException("REFUND_PAYMENT_NOT_READY","The Cashfree payment is not eligible for refund",HttpStatus.CONFLICT);
            com.mittiandmore.entity.Refund record=new com.mittiandmore.entity.Refund(); record.setReturnRequest(rr); record.setOrder(rr.getOrder()); record.setPayment(payment); record.setAmount(amount); record.setCurrency("INR"); record.setStatus("PENDING"); record.setReason(request.getReason()); record.setReceipt(receipt(rr));
            refundRepository.save(record);
            try {
                String gatewayRefundId = "cerclay_refund_" + returnId + "_" + System.currentTimeMillis();
                com.fasterxml.jackson.databind.JsonNode result = cashfreeGateway.createRefund(payment.getCashfreeOrderId(), gatewayRefundId, amount, request.getReason());
                record.setCashfreeRefundId(result.path("refund_id").asText(gatewayRefundId));
                record.setStatus(normalizeStatus(result.path("refund_status").asText("PENDING")));
                if ("PROCESSED".equals(record.getStatus())) record.setProcessedAt(LocalDateTime.now());
                rr.setRefundRequestedAmount(already.add(amount)); rr.setStatus("REFUND_PENDING"); returnRepository.save(rr); refundRepository.save(record);
                if ("PROCESSED".equals(record.getStatus())) { completeReturnIfSatisfied(record); notificationService.enqueue(NotificationEventType.REFUND_PROCESSED,rr.getOrder(),null,null); }
                return toResponse(record);
            } catch (Exception e) {
                record.setStatus("FAILED"); record.setFailureReason(trim(e.getMessage())); refundRepository.save(record);
                throw new ApiException("REFUND_INITIATION_FAILED","Unable to initiate the Cashfree refund",HttpStatus.BAD_GATEWAY);
            }
        }
        if(!"PAID".equalsIgnoreCase(payment.getPaymentStatus()) || payment.getRazorpayPaymentId()==null || payment.getRazorpayPaymentId().isBlank()) throw new ApiException("REFUND_PAYMENT_NOT_READY","The Razorpay payment is not eligible for refund",HttpStatus.CONFLICT);
        if(!razorpayProperties.isEnabled() || blank(razorpayProperties.getKeyId()) || blank(razorpayProperties.getKeySecret())) throw new ApiException("RAZORPAY_NOT_CONFIGURED","Razorpay is not configured",HttpStatus.SERVICE_UNAVAILABLE);

        com.mittiandmore.entity.Refund record=new com.mittiandmore.entity.Refund(); record.setReturnRequest(rr); record.setOrder(rr.getOrder()); record.setPayment(payment); record.setAmount(amount); record.setCurrency("INR"); record.setStatus("PENDING"); record.setReason(request.getReason()); record.setReceipt(receipt(rr));
        refundRepository.save(record);
        try {
            RazorpayClient client=new RazorpayClient(razorpayProperties.getKeyId(),razorpayProperties.getKeySecret());
            Refund existingGateway = findGatewayRefund(client, payment.getRazorpayPaymentId(), record.getReceipt());
            if (existingGateway != null) {
                record.setRazorpayRefundId(String.valueOf(existingGateway.get("id")));
                record.setStatus(normalizeStatus(String.valueOf(existingGateway.get("status"))));
                if ("PROCESSED".equals(record.getStatus())) record.setProcessedAt(LocalDateTime.now());
            } else {
            JSONObject options=new JSONObject(); options.put("amount",toPaise(amount)); options.put("receipt",record.getReceipt());
            options.put("notes",new JSONObject().put("cerclay_return_id",String.valueOf(returnId)).put("cerclay_order_id",String.valueOf(rr.getOrder().getId())));
            Refund gateway=client.payments.refund(payment.getRazorpayPaymentId(),options);
            record.setRazorpayRefundId(String.valueOf(gateway.get("id")));
            String status=String.valueOf(gateway.get("status"));
            record.setStatus(normalizeStatus(status));
            if("PROCESSED".equals(record.getStatus())) {record.setProcessedAt(LocalDateTime.now()); completeReturnIfSatisfied(record); notificationService.enqueue(NotificationEventType.REFUND_PROCESSED,rr.getOrder(),null,null);}
            }
            if ("PROCESSED".equals(record.getStatus())) {
                record.setProcessedAt(LocalDateTime.now());
            }
            rr.setRefundRequestedAmount(already.add(amount));
            rr.setStatus("REFUND_PENDING");
            returnRepository.save(rr);
            refundRepository.save(record);
            if ("PROCESSED".equals(record.getStatus())) {
                completeReturnIfSatisfied(record);
            }
            return toResponse(record);
        } catch (Exception e) {
            record.setStatus("FAILED"); record.setFailureReason(trim(e.getMessage())); refundRepository.save(record);
            throw new ApiException("REFUND_INITIATION_FAILED","Unable to initiate the Razorpay refund",HttpStatus.BAD_GATEWAY);
        }
    }

    @Transactional
    public RefundResponse refundCancelledOrder(Order order, String reason) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "Payment record not found", HttpStatus.CONFLICT));
        if ("CASHFREE".equalsIgnoreCase(order.getPaymentMethod()) && "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            if (payment.getCashfreeOrderId() == null || payment.getCashfreeOrderId().isBlank()) throw new ApiException("REFUND_PAYMENT_NOT_READY", "The Cashfree payment is not eligible for refund", HttpStatus.CONFLICT);
            BigDecimal already = refundRepository.sumSuccessfulOrPendingByOrderId(order.getId());
            BigDecimal remaining = money(order.getTotal().subtract(already));
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) throw new ApiException("REFUND_ALREADY_COMPLETED", "The order has already been fully refunded", HttpStatus.CONFLICT);
            com.mittiandmore.entity.Refund record = new com.mittiandmore.entity.Refund(); record.setOrder(order); record.setPayment(payment); record.setAmount(remaining); record.setCurrency("INR"); record.setStatus("PENDING"); record.setReason(reason); record.setReceipt("cerclay_cancel_refund_" + order.getId() + "_" + System.nanoTime());
            refundRepository.save(record);
            try {
                String gatewayRefundId = "cerclay_cancel_" + order.getId() + "_" + System.currentTimeMillis();
                com.fasterxml.jackson.databind.JsonNode result = cashfreeGateway.createRefund(payment.getCashfreeOrderId(), gatewayRefundId, remaining, reason);
                record.setCashfreeRefundId(result.path("refund_id").asText(gatewayRefundId));
                record.setStatus(normalizeStatus(result.path("refund_status").asText("PENDING")));
                if ("PROCESSED".equals(record.getStatus())) { record.setProcessedAt(LocalDateTime.now()); completeReturnIfSatisfied(record); }
                refundRepository.save(record);
                return toResponse(record);
            } catch (Exception e) {
                record.setStatus("FAILED"); record.setFailureReason(trim(e.getMessage())); refundRepository.save(record);
                throw new ApiException("REFUND_INITIATION_FAILED", "Unable to initiate the Cashfree refund", HttpStatus.BAD_GATEWAY);
            }
        }
        if (!"RAZORPAY".equalsIgnoreCase(order.getPaymentMethod()) || !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return null;
        }
        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) throw new ApiException("REFUND_PAYMENT_NOT_READY", "The Razorpay payment is not eligible for refund", HttpStatus.CONFLICT);
        BigDecimal already = refundRepository.sumSuccessfulOrPendingByOrderId(order.getId());
        BigDecimal remaining = money(order.getTotal().subtract(already));
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) throw new ApiException("REFUND_ALREADY_COMPLETED", "The order has already been fully refunded", HttpStatus.CONFLICT);
        if (!razorpayProperties.isEnabled() || blank(razorpayProperties.getKeyId()) || blank(razorpayProperties.getKeySecret())) throw new ApiException("RAZORPAY_NOT_CONFIGURED", "Razorpay is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        com.mittiandmore.entity.Refund record = new com.mittiandmore.entity.Refund(); record.setOrder(order); record.setPayment(payment); record.setAmount(remaining); record.setCurrency("INR"); record.setStatus("PENDING"); record.setReason(reason); record.setReceipt("cerclay_cancel_refund_" + order.getId() + "_" + System.nanoTime());
        refundRepository.save(record);
        try {
            RazorpayClient client = new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
            JSONObject options = new JSONObject(); options.put("amount", toPaise(remaining)); options.put("receipt", record.getReceipt());
            options.put("notes", new JSONObject().put("cerclay_order_id", String.valueOf(order.getId())).put("reason", "ORDER_CANCELLED"));
            Refund gateway = client.payments.refund(payment.getRazorpayPaymentId(), options);
            record.setRazorpayRefundId(String.valueOf(gateway.get("id"))); record.setStatus(normalizeStatus(String.valueOf(gateway.get("status"))));
            if ("PROCESSED".equals(record.getStatus())) { record.setProcessedAt(LocalDateTime.now()); payment.setPaymentStatus("REFUNDED"); paymentRepository.save(payment); }
            refundRepository.save(record); return toResponse(record);
        } catch (Exception e) { record.setStatus("FAILED"); record.setFailureReason(trim(e.getMessage())); refundRepository.save(record); throw new ApiException("REFUND_INITIATION_FAILED", "Unable to initiate the Razorpay refund", HttpStatus.BAD_GATEWAY); }
    }

    @Transactional
    public RefundResponse markManualProcessed(Long refundId) {
        com.mittiandmore.entity.Refund r=refundRepository.findByIdForUpdate(refundId).orElseThrow(()->new ApiException("REFUND_NOT_FOUND","Refund not found",HttpStatus.NOT_FOUND));
        if(!"MANUAL_PENDING".equals(r.getStatus())) throw new ApiException("INVALID_REFUND_STATE","Refund is not awaiting manual processing",HttpStatus.CONFLICT);
        r.setStatus("PROCESSED");r.setProcessedAt(LocalDateTime.now());refundRepository.save(r);completeReturnIfSatisfied(r);notificationService.enqueue(NotificationEventType.REFUND_PROCESSED,r.getOrder(),null,null);return toResponse(r);
    }

    @Transactional
    public void handleWebhook(String eventType, JSONObject entity) {
        String cashfreeRefundId=entity.optString("refund_id",null);
        if (!blank(cashfreeRefundId)) {
            com.mittiandmore.entity.Refund cashfree=refundRepository.findByCashfreeRefundId(cashfreeRefundId).orElse(null);
            if (cashfree != null) {
                String status=normalizeStatus(entity.optString("refund_status","PENDING"));
                cashfree.setStatus(status);
                if ("PROCESSED".equals(status)) { cashfree.setProcessedAt(LocalDateTime.now()); completeReturnIfSatisfied(cashfree); notificationService.enqueue(NotificationEventType.REFUND_PROCESSED,cashfree.getOrder(),null,null); }
                refundRepository.save(cashfree); return;
            }
        }
        String refundId=entity.optString("id",null); if(blank(refundId)) return;
        com.mittiandmore.entity.Refund r=refundRepository.findByRazorpayRefundId(refundId).orElse(null); if(r==null)return;
        if("refund.created".equals(eventType)) r.setStatus("PENDING");
        else if("refund.processed".equals(eventType)){r.setStatus("PROCESSED");r.setProcessedAt(LocalDateTime.now());completeReturnIfSatisfied(r);notificationService.enqueue(NotificationEventType.REFUND_PROCESSED,r.getOrder(),null,null);}
        else if("refund.failed".equals(eventType)){r.setStatus("FAILED");r.setFailureReason(entity.optString("failure_reason",entity.optString("description",null)));}
        refundRepository.save(r);
    }

    @Transactional(readOnly=true) public BigDecimal refundedAmount(Long orderId){return money(refundRepository.sumSuccessfulOrPendingByOrderId(orderId));}
    @Transactional(readOnly=true) public List<RefundResponse> forOrder(Long orderId){return refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::toResponse).toList();}
    @Transactional(readOnly=true) public List<RefundResponse> forReturn(Long returnId){return refundRepository.findByReturnRequestIdOrderByCreatedAtDesc(returnId).stream().map(this::toResponse).toList();}
    @Transactional(readOnly=true) public BigDecimal refundedAmountForReturn(Long returnId){return money(refundRepository.sumSuccessfulOrPendingByReturnId(returnId));}
    @Transactional(readOnly=true) public List<RefundResponse> all(){return refundRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();}

    private void completeReturnIfSatisfied(com.mittiandmore.entity.Refund refund){
        if (refund.getPayment() != null && "PROCESSED".equals(refund.getStatus())) {
            BigDecimal paid = refund.getPayment().getAmount();
            BigDecimal totalRefunded = refundRepository.sumSuccessfulOrPendingByOrderId(refund.getOrder().getId());
            if (totalRefunded.compareTo(paid) >= 0) {
                refund.getPayment().setPaymentStatus("REFUNDED");
                paymentRepository.save(refund.getPayment());
            }
        }
        ReturnRequest rr=refund.getReturnRequest(); if(rr==null)return;
        BigDecimal requested=rr.getRefundRequestedAmount(); if(requested==null)return;
        BigDecimal done=refundRepository.sumSuccessfulOrPendingByReturnId(rr.getId());
        if(done.compareTo(requested)>=0 && "REFUND_PENDING".equals(rr.getStatus())){rr.setStatus("REFUNDED");rr.setClosedAt(LocalDateTime.now());returnRepository.save(rr);rr.getOrder().setOrderStatus("REFUNDED");orderRepository.save(rr.getOrder());}
    }
    private RefundResponse toResponse(com.mittiandmore.entity.Refund r){RefundResponse x=new RefundResponse();x.setId(r.getId());x.setOrderId(r.getOrder().getId());if(r.getReturnRequest()!=null)x.setReturnRequestId(r.getReturnRequest().getId());x.setRazorpayRefundId(r.getRazorpayRefundId());x.setCashfreeRefundId(r.getCashfreeRefundId());x.setAmount(r.getAmount());x.setCurrency(r.getCurrency());x.setStatus(r.getStatus());x.setReason(r.getReason());x.setReceipt(r.getReceipt());x.setFailureReason(r.getFailureReason());x.setCreatedAt(r.getCreatedAt());x.setUpdatedAt(r.getUpdatedAt());x.setProcessedAt(r.getProcessedAt());return x;}
    private Refund findGatewayRefund(RazorpayClient client, String paymentId, String receipt) {
        try {
            for (Refund refund : client.payments.fetchAllRefunds(paymentId)) {
                if (receipt.equals(refund.get("receipt"))) return refund;
            }
        } catch (Exception ignored) {
            // Creation is still attempted if the reconciliation lookup is unavailable.
        }
        return null;
    }
    private String receipt(ReturnRequest rr){return "cerclay_refund_"+rr.getId()+"_"+System.nanoTime();}
    private long toPaise(BigDecimal a){return a.setScale(2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).longValueExact();}
    private BigDecimal money(BigDecimal a){return (a==null?BigDecimal.ZERO:a).setScale(2,RoundingMode.HALF_UP);}
    private String normalizeStatus(String s){return ("processed".equalsIgnoreCase(s)||"success".equalsIgnoreCase(s))?"PROCESSED":(("failed".equalsIgnoreCase(s)||"failure".equalsIgnoreCase(s))?"FAILED":"PENDING");}
    private String trim(String s){return s==null?"Unknown refund gateway error":s.substring(0,Math.min(1000,s.length()));}
    private boolean blank(String s){return s==null||s.isBlank();}
}
