package com.mittiandmore.repository;

import com.mittiandmore.entity.ProductReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, String status);
    List<ProductReview> findAllByOrderByCreatedAtDesc();
    List<ProductReview> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ProductReview> findTop6ByStatusOrderByCreatedAtDesc(String status);
    Optional<ProductReview> findByCustomerIdAndProductId(Long customerId, Long productId);

    @Query(
        "select case when count(oi) > 0 then true else false end from OrderItem oi where oi.order.customer.id = :customerId and oi.product.id = :productId and oi.order.deliveredAt is not null"
    )
    boolean hasDeliveredPurchase(@Param("customerId") Long customerId, @Param("productId") Long productId);

    long countByProductIdAndStatus(Long productId, String status);
    long deleteByProductId(Long productId);
}
