package com.mittiandmore.controller;

import com.mittiandmore.dto.ShipmentResponse;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.entity.ShipmentTrackingEvent;
import com.mittiandmore.dto.ShipmentTrackingEventResponse;
import com.mittiandmore.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShipmentResponse> getShipmentByOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                toResponse(shipmentService.getShipmentByOrderId(orderId))
        );
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber
    ) {
        return ResponseEntity.ok(
                toResponse(
                        shipmentService.getShipmentByTrackingNumber(
                                trackingNumber
                        )
                )
        );
    }

    @GetMapping("/{shipmentId}/timeline")
    public ResponseEntity<java.util.List<ShipmentTrackingEventResponse>> getTimeline(
            @PathVariable Long shipmentId) {
        return ResponseEntity.ok(
                shipmentService.getShipmentTimeline(shipmentId).stream().map(event -> {
                    ShipmentTrackingEventResponse response = new ShipmentTrackingEventResponse();
                    response.setShipmentStatus(event.getShipmentStatus());
                    response.setExternalStatus(event.getExternalStatus());
                    response.setExternalStatusDisplay(event.getExternalStatusDisplay());
                    response.setLocation(event.getLocation());
                    response.setRemarks(event.getRemarks());
                    response.setEventAt(event.getEventAt());
                    response.setSource(event.getSource());
                    return response;
                }).toList()
        );
    }

    @PatchMapping("/{shipmentId}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Long shipmentId,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(
                toResponse(
                        shipmentService.updateShipmentStatus(
                                shipmentId,
                                status
                        )
                )
        );
    }

    @PatchMapping("/{shipmentId}/courier")
    public ResponseEntity<ShipmentResponse> assignCourier(
            @PathVariable Long shipmentId,
            @RequestParam String courierName,
            @RequestParam(required = false) String trackingNumber
    ) {
        return ResponseEntity.ok(
                toResponse(
                        shipmentService.assignCourier(
                                shipmentId,
                                courierName,
                                trackingNumber
                        )
                )
        );
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
