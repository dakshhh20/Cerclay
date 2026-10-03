package com.mittiandmore.controller;

import com.mittiandmore.dto.OrderResponse;
import com.mittiandmore.dto.CancelOrderRequest;
import com.mittiandmore.dto.OrderStatusRequest;
import jakarta.validation.Valid;
import com.mittiandmore.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns all customer orders for the admin order-management screen.
     * The endpoint is protected by the /api/admin/** security rule.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId, @Valid @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getStatus()));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {

        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(
                orderService.cancelOrder(orderId, reason, "ADMIN")
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }
}
