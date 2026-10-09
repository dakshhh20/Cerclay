package com.mittiandmore.controller;

import com.mittiandmore.dto.ShipmentResponse;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.service.ShadowfaxTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shadowfax")
public class ShadowfaxTrackingController {

    private final ShadowfaxTrackingService shadowfaxTrackingService;

    public ShadowfaxTrackingController(ShadowfaxTrackingService shadowfaxTrackingService) {
        this.shadowfaxTrackingService = shadowfaxTrackingService;
    }

    /**
     * Admin-only manual tracking sync. This is useful as a fallback even
     * after webhooks are added later.
     */
    @PostMapping("/shipments/{shipmentId}/sync")
    public ResponseEntity<ShipmentResponse> syncShipment(@PathVariable Long shipmentId) {
        Shipment shipment = shadowfaxTrackingService.syncShipmentTracking(shipmentId);

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
