package com.mittiandmore.repository;

import com.mittiandmore.entity.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {
    boolean existsByEventKey(String eventKey);
    List<ShipmentTrackingEvent> findByShipmentIdOrderByEventAtAscCreatedAtAsc(Long shipmentId);
}
