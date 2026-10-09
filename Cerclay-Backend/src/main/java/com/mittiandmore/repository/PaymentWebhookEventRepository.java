package com.mittiandmore.repository;

import com.mittiandmore.entity.PaymentWebhookEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByEventId(String eventId);
}
