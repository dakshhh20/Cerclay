package com.mittiandmore.repository;

import com.mittiandmore.entity.ReturnStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, Long> {
    List<ReturnStatusHistory> findByReturnRequestIdOrderByCreatedAtAsc(Long returnRequestId);
}
