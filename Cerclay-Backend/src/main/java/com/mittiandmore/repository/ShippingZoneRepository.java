package com.mittiandmore.repository;

import com.mittiandmore.entity.ShippingZone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {
    Optional<ShippingZone> findByCode(String code);
    List<ShippingZone> findAllByOrderByCodeAsc();
}
