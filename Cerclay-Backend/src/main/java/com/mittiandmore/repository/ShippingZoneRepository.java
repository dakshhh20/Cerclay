package com.mittiandmore.repository;

import com.mittiandmore.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {
    Optional<ShippingZone> findByCode(String code);
    List<ShippingZone> findAllByOrderByCodeAsc();
}
