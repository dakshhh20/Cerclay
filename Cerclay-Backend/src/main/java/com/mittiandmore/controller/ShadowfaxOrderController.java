package com.mittiandmore.controller;

import com.mittiandmore.dto.ShipmentResponse;
import com.mittiandmore.dto.ShadowfaxAwbRequest;
import jakarta.validation.Valid;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.service.ShadowfaxOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shadowfax")
public class ShadowfaxOrderController {

    private final ShadowfaxOrderService shadowfaxOrderService;

    public ShadowfaxOrderController(
            ShadowfaxOrderService shadowfaxOrderService
    ) {
        this.shadowfaxOrderService = shadowfaxOrderService;
    }

    /**
     * Admin-controlled shipment creation.
     *
     * The customer only creates the ecommerce order. A Shadowfax
     * shipment is created later when an authenticated admin explicitly
     * triggers this endpoint.
     */
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<ShipmentResponse> createShipmentForOrder(
            @PathVariable Long orderId
    ) {
        Shipment shipment =
                shadowfaxOrderService.createShipmentForOrder(orderId);

        return ResponseEntity.ok(toResponse(shipment));
    }

    /**
     * Links a shipment that was created manually in the Shadowfax dashboard
     * to an existing Cerclay order, then immediately attempts the first
     * tracking sync. The AWB is kept even if Shadowfax has not exposed the
     * shipment to its tracking API yet.
     */
    @PostMapping("/orders/{orderId}/awb")
    public ResponseEntity<ShipmentResponse> assignManualAwb(
            @PathVariable Long orderId,
            @Valid @RequestBody ShadowfaxAwbRequest request
    ) {
        Shipment shipment = shadowfaxOrderService.assignManualShadowfaxAwbAndSync(
                orderId,
                request.getAwb()
        );

        return ResponseEntity.ok(toResponse(shipment));
    }

    private ShipmentResponse toResponse(Shipment shipment) {
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
}
