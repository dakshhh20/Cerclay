package com.mittiandmore.repository;

import com.mittiandmore.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByGuestId(String guestId);

    Optional<Cart> findByCustomerId(Long customerId);
}
