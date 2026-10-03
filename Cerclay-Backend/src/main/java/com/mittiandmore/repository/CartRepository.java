package com.mittiandmore.repository;

import com.mittiandmore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByGuestId(String guestId);

    Optional<Cart> findByCustomerId(Long customerId);
}