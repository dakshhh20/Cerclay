package com.mittiandmore.service;

import com.mittiandmore.entity.Shipment;
import com.mittiandmore.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShadowfaxTrackingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ShadowfaxTrackingScheduler.class);

    private final ShipmentRepository shipmentRepository;
    private final ShadowfaxTrackingService trackingService;

    public ShadowfaxTrackingScheduler(
            ShipmentRepository shipmentRepository,
            ShadowfaxTrackingService trackingService) {
        this.shipmentRepository = shipmentRepository;
        this.trackingService = trackingService;
    }

    /**
     * Poll only active Shadowfax shipments. Terminal shipments are intentionally
     * excluded so polling does not waste provider calls on historical orders.
     * The admin manual sync endpoint remains available as an immediate fallback.
     */
    @Scheduled(fixedDelayString = "${shadowfax.tracking.poll-ms:900000}")
    public void syncActiveShipments() {
        List<Shipment> shipments = shipmentRepository
                .findTop100ByCourierNameIgnoreCaseAndShipmentStatusInOrderByLastSyncedAtAsc(
                        "Shadowfax",
                        List.of("CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERY_ATTEMPTED")
                );

        for (Shipment shipment : shipments) {
            try {
                trackingService.syncShipmentTracking(shipment.getId());
            } catch (Exception ex) {
                log.warn("Shadowfax scheduled sync failed for shipment {}: {}",
                        shipment.getId(), ex.getMessage());
            }
        }
    }
}
