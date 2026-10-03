package com.mittiandmore.repository;
import com.mittiandmore.entity.Refund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.*;
public interface RefundRepository extends JpaRepository<Refund,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Refund r where r.id=:id") Optional<Refund> findByIdForUpdate(@Param("id") Long id);
 Optional<Refund> findByRazorpayRefundId(String id);
 Optional<Refund> findByCashfreeRefundId(String id);
 Optional<Refund> findByReceipt(String receipt);
 @Query("select coalesce(sum(r.amount),0) from Refund r where r.order.id=:orderId and r.status in ('PENDING','PROCESSED')") BigDecimal sumSuccessfulOrPendingByOrderId(@Param("orderId") Long orderId);
 List<Refund> findByOrderIdOrderByCreatedAtDesc(Long orderId);
 @Query("select coalesce(sum(r.amount),0) from Refund r where r.returnRequest.id=:returnId and r.status in ('PENDING','PROCESSED')") BigDecimal sumSuccessfulOrPendingByReturnId(@Param("returnId") Long returnId);
 List<Refund> findByReturnRequestIdOrderByCreatedAtDesc(Long returnId);
 List<Refund> findAllByOrderByCreatedAtDesc();
}
