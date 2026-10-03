package com.mittiandmore.repository;

import com.mittiandmore.entity.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findTop100ByOrderByCreatedAtDesc();
    List<InventoryAdjustment> findTop100ByProductIdOrderByCreatedAtDesc(Long productId);
    long deleteByProductId(Long productId);

}