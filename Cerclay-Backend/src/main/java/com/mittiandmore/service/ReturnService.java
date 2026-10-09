package com.mittiandmore.service;

import com.mittiandmore.dto.*;
import com.mittiandmore.entity.*;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.notification.NotificationEventType;
import com.mittiandmore.notification.NotificationService;
import com.mittiandmore.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {

    private static final Set<String> ACTIVE_STATUSES = Set.of("REQUESTED", "APPROVED", "RECEIVED", "REFUND_PENDING");

    private final ReturnRequestRepository returnRepository;
    private final ReturnStatusHistoryRepository historyRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final RefundService refundService;
    private final ReturnPhotoRepository photoRepository;
    private final ReturnPhotoStorageService photoStorage;

    public ReturnService(
        ReturnRequestRepository returnRepository,
        ReturnStatusHistoryRepository historyRepository,
        OrderRepository orderRepository,
        InventoryService inventoryService,
        NotificationService notificationService,
        RefundService refundService,
        ReturnPhotoRepository photoRepository,
        ReturnPhotoStorageService photoStorage
    ) {
        this.returnRepository = returnRepository;
        this.historyRepository = historyRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.refundService = refundService;
        this.photoRepository = photoRepository;
        this.photoStorage = photoStorage;
    }

    @Transactional
    public ReturnResponse create(Long customerId, Long orderId, CreateReturnRequest request) {
        Order order = orderRepository
            .findByIdForUpdate(orderId)
            .orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Order not found"));
        if (!order.getCustomer().getId().equals(customerId)) throw new ApiException(
            "RETURN_ACCESS_DENIED",
            "You are not allowed to return this order",
            HttpStatus.FORBIDDEN
        );
        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) throw new ApiException(
            "RETURN_NOT_ELIGIBLE",
            "Only delivered orders can be returned",
            HttpStatus.BAD_REQUEST
        );
        LocalDateTime deliveredAt = order.getDeliveredAt();
        if (deliveredAt == null || deliveredAt.plusDays(2).isBefore(LocalDateTime.now())) throw new ApiException(
            "RETURN_WINDOW_EXPIRED",
            "Returns are available within 2 days of delivery",
            HttpStatus.BAD_REQUEST
        );
        Set<String> allowedReasons = Set.of("DAMAGED", "DEFECTIVE", "WRONG_ITEM", "MISSING_ITEM");
        String reason = request.getReason() == null ? "" : request.getReason().trim().toUpperCase(Locale.ROOT);
        if (!allowedReasons.contains(reason)) throw new ApiException(
            "RETURN_REASON_NOT_ALLOWED",
            "Only damaged, defective, wrong item or missing item returns are allowed",
            HttpStatus.BAD_REQUEST
        );
        if (
            returnRepository.findFirstByOrderIdAndStatusNotIn(orderId, Set.of("REJECTED", "CLOSED")).isPresent()
        ) throw new ApiException(
            "RETURN_ALREADY_EXISTS",
            "An active return already exists for this order",
            HttpStatus.CONFLICT
        );

        ReturnRequest rr = new ReturnRequest();
        rr.setOrder(order);
        rr.setCustomer(order.getCustomer());
        rr.setStatus("REQUESTED");
        rr.setReason(reason);
        rr.setCustomerNote(request.getCustomerNote());

        Map<Long, Integer> requested = new HashMap<>();
        for (ReturnItemRequest itemRequest : request.getItems()) {
            if (
                itemRequest.getOrderItemId() == null ||
                itemRequest.getQuantity() == null ||
                itemRequest.getQuantity() <= 0
            ) throw new ApiException(
                "INVALID_RETURN_ITEM",
                "Return item quantity must be positive",
                HttpStatus.BAD_REQUEST
            );
            if (requested.put(itemRequest.getOrderItemId(), itemRequest.getQuantity()) != null) throw new ApiException(
                "DUPLICATE_RETURN_ITEM",
                "The same order item cannot be listed twice",
                HttpStatus.BAD_REQUEST
            );
            OrderItem item = order
                .getItems()
                .stream()
                .filter(x -> x.getId().equals(itemRequest.getOrderItemId()))
                .findFirst()
                .orElseThrow(() ->
                    new ApiException(
                        "ORDER_ITEM_NOT_FOUND",
                        "Order item does not belong to this order",
                        HttpStatus.BAD_REQUEST
                    )
                );
            if (itemRequest.getQuantity() > item.getQuantity()) throw new ApiException(
                "RETURN_QUANTITY_EXCEEDED",
                "Return quantity exceeds ordered quantity for " + item.getProductName(),
                HttpStatus.BAD_REQUEST
            );
            ReturnItem ri = new ReturnItem();
            ri.setReturnRequest(rr);
            ri.setOrderItem(item);
            ri.setQuantity(itemRequest.getQuantity());
            rr.getItems().add(ri);
        }
        if (rr.getItems().isEmpty()) throw new ApiException(
            "RETURN_ITEMS_REQUIRED",
            "At least one item is required",
            HttpStatus.BAD_REQUEST
        );
        returnRepository.save(rr);
        recordStatus(rr, null, "REQUESTED", reason, "CUSTOMER");
        notificationService.enqueue(NotificationEventType.RETURN_REQUESTED, order, null, null);
        return toResponse(rr);
    }

    @Transactional
    public ReturnResponse addPhotos(
        Long customerId,
        Long returnId,
        java.util.List<org.springframework.web.multipart.MultipartFile> files
    ) {
        ReturnRequest rr = returnRepository
            .findByIdForUpdate(returnId)
            .orElseThrow(() -> notFound("RETURN_NOT_FOUND", "Return request not found"));
        if (!rr.getCustomer().getId().equals(customerId)) throw new ApiException(
            "RETURN_ACCESS_DENIED",
            "You are not allowed to update this return",
            HttpStatus.FORBIDDEN
        );
        if (!Set.of("REQUESTED").contains(rr.getStatus())) throw new ApiException(
            "RETURN_PHOTO_WINDOW_CLOSED",
            "Photos can only be added while the return is being requested",
            HttpStatus.CONFLICT
        );
        if (files == null || files.isEmpty()) throw new ApiException(
            "RETURN_PHOTOS_REQUIRED",
            "At least one return photo is required",
            HttpStatus.BAD_REQUEST
        );
        long existing = photoRepository.countByReturnRequestId(returnId);
        if (existing + files.size() > 5) throw new ApiException(
            "RETURN_PHOTO_LIMIT",
            "You can upload up to 5 return photos",
            HttpStatus.BAD_REQUEST
        );
        try {
            for (var file : files) {
                var stored = photoStorage.store(returnId, file);
                ReturnPhoto photo = new ReturnPhoto();
                photo.setReturnRequest(rr);
                photo.setFileName(stored.fileName());
                photo.setOriginalFileName(stored.originalFileName());
                photo.setContentType(stored.contentType());
                rr.getPhotos().add(photo);
                photoRepository.save(photo);
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException("INVALID_RETURN_PHOTO", e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return toResponse(rr);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> customerReturns(Long customerId) {
        return returnRepository
            .findByCustomerIdOrderByRequestedAtDesc(customerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReturnResponse getCustomerReturn(Long customerId, Long id) {
        ReturnRequest rr = returnRepository
            .findById(id)
            .orElseThrow(() -> notFound("RETURN_NOT_FOUND", "Return request not found"));
        if (!rr.getCustomer().getId().equals(customerId)) throw new ApiException(
            "RETURN_ACCESS_DENIED",
            "You are not allowed to view this return",
            HttpStatus.FORBIDDEN
        );
        return toResponse(rr);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> all() {
        return returnRepository.findAllByOrderByRequestedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReturnResponse get(Long id) {
        return toResponse(
            returnRepository.findById(id).orElseThrow(() -> notFound("RETURN_NOT_FOUND", "Return request not found"))
        );
    }

    @Transactional
    public ReturnResponse approve(Long id, String note) {
        ReturnRequest rr = locked(id);
        requireStatus(rr, "REQUESTED");
        if (photoRepository.countByReturnRequestId(id) < 1) throw new ApiException(
            "RETURN_PHOTOS_REQUIRED",
            "At least one return photo is required before approval",
            HttpStatus.CONFLICT
        );
        rr.setAdminNote(note);
        rr.setApprovedAt(LocalDateTime.now());
        saveStatus(rr, "APPROVED", note, "ADMIN");
        rr.getOrder().setOrderStatus("RETURN_APPROVED");
        orderRepository.save(rr.getOrder());
        notificationService.enqueue(NotificationEventType.RETURN_APPROVED, rr.getOrder(), null, null);
        return toResponse(rr);
    }

    @Transactional
    public ReturnResponse reject(Long id, String note) {
        ReturnRequest rr = locked(id);
        requireStatus(rr, "REQUESTED");
        rr.setAdminNote(note);
        rr.setRejectedAt(LocalDateTime.now());
        saveStatus(rr, "REJECTED", note, "ADMIN");
        notificationService.enqueue(NotificationEventType.RETURN_REJECTED, rr.getOrder(), null, null);
        return toResponse(rr);
    }

    @Transactional
    public ReturnResponse markReceived(Long id, String note) {
        ReturnRequest rr = locked(id);
        requireStatus(rr, "APPROVED");
        for (ReturnItem item : rr.getItems())
            inventoryService.restoreStockForReturn(
                item.getOrderItem().getProduct().getId(),
                item.getQuantity() * Math.max(1, item.getOrderItem().getPackSize()),
                rr.getOrder().getId()
            );
        rr.setReceivedAt(LocalDateTime.now());
        rr.setAdminNote(note);
        saveStatus(rr, "RECEIVED", note, "ADMIN");
        rr.getOrder().setOrderStatus("RETURN_RECEIVED");
        orderRepository.save(rr.getOrder());
        notificationService.enqueue(NotificationEventType.RETURN_RECEIVED, rr.getOrder(), null, null);
        return toResponse(rr);
    }

    @Transactional
    public ReturnResponse markClosed(Long id, String note) {
        ReturnRequest rr = locked(id);
        if (!Set.of("REFUNDED", "REJECTED").contains(rr.getStatus())) throw new ApiException(
            "RETURN_NOT_CLOSABLE",
            "Return cannot be closed in its current state",
            HttpStatus.CONFLICT
        );
        rr.setClosedAt(LocalDateTime.now());
        rr.setAdminNote(note);
        saveStatus(rr, "CLOSED", note, "ADMIN");
        return toResponse(rr);
    }

    private ReturnRequest locked(Long id) {
        return returnRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> notFound("RETURN_NOT_FOUND", "Return request not found"));
    }

    private void requireStatus(ReturnRequest rr, String expected) {
        if (!expected.equals(rr.getStatus())) throw new ApiException(
            "INVALID_RETURN_STATE",
            "Return must be in " + expected + " state",
            HttpStatus.CONFLICT
        );
    }

    private void recordStatus(ReturnRequest rr, String from, String to, String note, String actor) {
        ReturnStatusHistory h = new ReturnStatusHistory();
        h.setReturnRequest(rr);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setNote(note);
        h.setActor(actor);
        historyRepository.save(h);
    }

    private void saveStatus(ReturnRequest rr, String to, String note, String actor) {
        String from = rr.getStatus();
        rr.setStatus(to);
        returnRepository.save(rr);
        recordStatus(rr, from, to, note, actor);
    }

    public ReturnResponse toResponse(ReturnRequest rr) {
        ReturnResponse r = new ReturnResponse();
        r.setId(rr.getId());
        r.setOrderId(rr.getOrder().getId());
        r.setCustomerId(rr.getCustomer().getId());
        r.setOrderNumber(rr.getOrder().getOrderNumber());
        r.setStatus(rr.getStatus());
        r.setReason(rr.getReason());
        r.setCustomerNote(rr.getCustomerNote());
        r.setAdminNote(rr.getAdminNote());
        r.setRequestedAt(rr.getRequestedAt());
        r.setApprovedAt(rr.getApprovedAt());
        r.setRejectedAt(rr.getRejectedAt());
        r.setReceivedAt(rr.getReceivedAt());
        r.setClosedAt(rr.getClosedAt());
        r.setRefundRequestedAmount(rr.getRefundRequestedAmount());
        BigDecimal estimated = BigDecimal.ZERO;
        for (ReturnItem i : rr.getItems()) {
            estimated = estimated.add(i.getOrderItem().getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            ReturnItemResponse x = new ReturnItemResponse();
            x.setId(i.getId());
            x.setOrderItemId(i.getOrderItem().getId());
            x.setProductId(i.getOrderItem().getProduct().getId());
            x.setProductName(i.getOrderItem().getProductName());
            x.setSku(i.getOrderItem().getProductSku());
            x.setQuantity(i.getQuantity());
            x.setUnitPrice(i.getOrderItem().getUnitPrice());
            r.getItems().add(x);
        }
        estimated = estimated.setScale(2, RoundingMode.HALF_UP);
        r.setEstimatedRefundAmount(estimated);
        for (ReturnPhoto p : photoRepository.findByReturnRequestIdOrderByCreatedAtAsc(rr.getId())) {
            ReturnPhotoResponse x = new ReturnPhotoResponse();
            x.setId(p.getId());
            x.setOriginalFileName(p.getOriginalFileName());
            x.setUrl("/uploads/returns/" + rr.getId() + "/" + p.getFileName());
            r.getPhotos().add(x);
        }
        BigDecimal refunded = refundService.refundedAmountForReturn(rr.getId());
        r.setRefundedAmount(refunded);
        r.setRefundableAmount(estimated.subtract(refunded).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        r.setRefunds(refundService.forReturn(rr.getId()));
        return r;
    }

    private ApiException notFound(String code, String msg) {
        return new ApiException(code, msg, HttpStatus.NOT_FOUND);
    }
}
