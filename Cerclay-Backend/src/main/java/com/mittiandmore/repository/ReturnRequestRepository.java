package com.mittiandmore.repository;

import com.mittiandmore.entity.ReturnRequest;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReturnRequest r where r.id=:id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<ReturnRequest> findFirstByOrderIdAndStatusNotIn(Long orderId, Collection<String> statuses);
    List<ReturnRequest> findAllByOrderByRequestedAtDesc();
    List<ReturnRequest> findByCustomerIdOrderByRequestedAtDesc(Long customerId);
}
