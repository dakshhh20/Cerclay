package com.mittiandmore.repository;

import com.mittiandmore.entity.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<WishlistItem> findByCustomerIdAndProductId(Long customerId, Long productId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);
    long deleteByProductId(Long productId);
}
