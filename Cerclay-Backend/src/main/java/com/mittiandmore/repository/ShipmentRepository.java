package com.mittiandmore.repository;

import com.mittiandmore.entity.Shipment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findTop100ByCourierNameIgnoreCaseAndShipmentStatusInOrderByLastSyncedAtAsc(
        String courierName,
        List<String> shipmentStatuses
    );
}
