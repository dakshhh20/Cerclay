package com.mittiandmore.repository;

import com.mittiandmore.entity.ShippingPincode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingPincodeRepository extends JpaRepository<ShippingPincode, Long> {
    Optional<ShippingPincode> findByPincode(String pincode);
    boolean existsByPincode(String pincode);
    List<ShippingPincode> findAllByOrderByPincodeAsc();
}
