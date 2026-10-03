package com.mittiandmore.repository;

import com.mittiandmore.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByEventId(String eventId);
}
