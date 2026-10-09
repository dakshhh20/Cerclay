package com.mittiandmore.repository;

import com.mittiandmore.entity.ShipmentTrackingEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {
    boolean existsByEventKey(String eventKey);
    List<ShipmentTrackingEvent> findByShipmentIdOrderByEventAtAscCreatedAtAsc(Long shipmentId);
}
