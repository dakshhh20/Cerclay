package com.mittiandmore.repository;
import com.mittiandmore.entity.ReturnStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory,Long>{ List<ReturnStatusHistory> findByReturnRequestIdOrderByCreatedAtAsc(Long returnRequestId); }
