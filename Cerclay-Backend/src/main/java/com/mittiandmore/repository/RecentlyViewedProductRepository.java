package com.mittiandmore.repository;

import com.mittiandmore.entity.RecentlyViewedProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, Long> {
    List<RecentlyViewedProduct> findTop12ByCustomerIdAndProductActiveTrueOrderByLastViewedAtDesc(Long customerId);

    Optional<RecentlyViewedProduct> findByCustomerIdAndProductId(Long customerId, Long productId);
    long deleteByProductId(Long productId);
}
