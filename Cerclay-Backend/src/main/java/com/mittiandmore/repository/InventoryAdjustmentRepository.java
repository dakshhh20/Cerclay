package com.mittiandmore.repository;

import com.mittiandmore.entity.InventoryAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findTop100ByOrderByCreatedAtDesc();
    List<InventoryAdjustment> findTop100ByProductIdOrderByCreatedAtDesc(Long productId);
    long deleteByProductId(Long productId);
}
