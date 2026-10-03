package com.mittiandmore.repository;

import com.mittiandmore.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findTop100ByCourierNameIgnoreCaseAndShipmentStatusInOrderByLastSyncedAtAsc(
            String courierName, List<String> shipmentStatuses);
}