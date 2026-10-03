package com.mittiandmore.repository;

import com.mittiandmore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    long deleteByProductId(Long productId);
}
