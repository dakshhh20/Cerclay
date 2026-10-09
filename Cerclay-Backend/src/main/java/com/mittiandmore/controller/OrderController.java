package com.mittiandmore.controller;

import com.mittiandmore.dto.CancelOrderRequest;
import com.mittiandmore.dto.OrderCreateRequest;
import com.mittiandmore.dto.OrderResponse;
import com.mittiandmore.dto.ShipmentResponse;
import com.mittiandmore.dto.ShipmentTrackingEventResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.entity.ShipmentTrackingEvent;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.OrderService;
import com.mittiandmore.service.ShipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final ShipmentService shipmentService;

    public OrderController(
        OrderService orderService,
        CustomerRepository customerRepository,
        ShipmentService shipmentService
    ) {
        this.orderService = orderService;
        this.customerRepository = customerRepository;
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody OrderCreateRequest request,
        Authentication authentication
    ) {
        Long customerId = getAuthenticatedCustomerId(authentication);

        OrderResponse order = orderService.createOrder(customerId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) CancelOrderRequest request,
        Authentication authentication
    ) {
        Long customerId = getAuthenticatedCustomerId(authentication);
        OrderResponse existing = orderService.getOrderById(id);
        if (!existing.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(orderService.cancelOrder(id, reason, "CUSTOMER"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id, Authentication authentication) {
        Long customerId = getAuthenticatedCustomerId(authentication);

        OrderResponse order = orderService.getOrderById(id);

        if (!order.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(
        @PathVariable String orderNumber,
        Authentication authentication
    ) {
        Long customerId = getAuthenticatedCustomerId(authentication);

        OrderResponse order = orderService.getOrderByNumber(orderNumber);

        if (!order.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        Long customerId = getAuthenticatedCustomerId(authentication);

        return ResponseEntity.ok(orderService.getCustomerOrders(customerId));
    }

    /**
     * Customer-facing shipment/tracking information for an order they own.
     * Shipment creation remains admin-only.
     */
    @GetMapping("/{id}/shipment")
    public ResponseEntity<ShipmentResponse> getMyOrderShipment(@PathVariable Long id, Authentication authentication) {
        Long customerId = getAuthenticatedCustomerId(authentication);

        OrderResponse order = orderService.getOrderById(id);

        if (!order.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Shipment shipment = shipmentService.getShipmentByOrderId(id);

        return ResponseEntity.ok(toShipmentResponse(shipment));
    }

    @GetMapping("/{id}/shipment/timeline")
    public ResponseEntity<List<ShipmentTrackingEventResponse>> getMyOrderShipmentTimeline(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long customerId = getAuthenticatedCustomerId(authentication);
        OrderResponse order = orderService.getOrderById(id);
        if (!order.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Shipment shipment = shipmentService.getShipmentByOrderId(id);
        List<ShipmentTrackingEvent> events = shipmentService.getShipmentTimeline(shipment.getId());
        return ResponseEntity.ok(events.stream().map(this::toTrackingEventResponse).toList());
    }

    private ShipmentTrackingEventResponse toTrackingEventResponse(ShipmentTrackingEvent event) {
        ShipmentTrackingEventResponse response = new ShipmentTrackingEventResponse();
        response.setShipmentStatus(event.getShipmentStatus());
        response.setExternalStatus(event.getExternalStatus());
        response.setExternalStatusDisplay(event.getExternalStatusDisplay());
        response.setLocation(event.getLocation());
        response.setRemarks(event.getRemarks());
        response.setEventAt(event.getEventAt());
        response.setSource(event.getSource());
        return response;
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment) {
        ShipmentResponse response = new ShipmentResponse();

        response.setId(shipment.getId());
        response.setOrderId(shipment.getOrder().getId());
        response.setCourierName(shipment.getCourierName());
        response.setProviderCode(shipment.getProviderCode());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setShipmentStatus(shipment.getShipmentStatus());
        response.setExternalStatus(shipment.getExternalStatus());
        response.setExternalStatusDisplay(shipment.getExternalStatusDisplay());
        response.setCurrentLocation(shipment.getCurrentLocation());
        response.setLatestTrackingComment(shipment.getLatestTrackingComment());
        response.setCustomerTrackUrl(shipment.getCustomerTrackUrl());
        response.setLastEventAt(shipment.getLastEventAt());
        response.setLastSyncedAt(shipment.getLastSyncedAt());
        response.setShippedAt(shipment.getShippedAt());
        response.setDeliveredAt(shipment.getDeliveredAt());
        response.setCreatedAt(shipment.getCreatedAt());
        response.setUpdatedAt(shipment.getUpdatedAt());

        return response;
    }

    private Long getAuthenticatedCustomerId(Authentication authentication) {
        String email = authentication.getName();

        Customer customer = customerRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));

        return customer.getId();
    }
}
