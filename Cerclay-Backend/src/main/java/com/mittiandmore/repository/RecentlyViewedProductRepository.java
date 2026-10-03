package com.mittiandmore.repository;

import com.mittiandmore.entity.RecentlyViewedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, Long> {

    List<RecentlyViewedProduct> findTop12ByCustomerIdAndProductActiveTrueOrderByLastViewedAtDesc(Long customerId);

    Optional<RecentlyViewedProduct> findByCustomerIdAndProductId(Long customerId, Long productId);
    long deleteByProductId(Long productId);

}