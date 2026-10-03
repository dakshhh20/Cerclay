package com.mittiandmore.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.status in ('PENDING','FAILED') and (n.nextAttemptAt is null or n.nextAttemptAt <= :now) order by n.createdAt asc")
    List<Notification> findReady(LocalDateTime now, Pageable pageable);
    List<Notification> findTop100ByOrderByCreatedAtDesc();
}
