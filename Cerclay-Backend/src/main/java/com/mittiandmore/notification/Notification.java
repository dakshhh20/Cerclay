package com.mittiandmore.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_status_next_attempt", columnList = "status,next_attempt_at"),
        @Index(name = "idx_notifications_customer", columnList = "customer_id"),
        @Index(name = "idx_notifications_order", columnList = "order_id"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel = "EMAIL";

    @Column(name = "recipient_email", nullable = false, length = 320)
    private String recipientEmail;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Lob @Column(name = "body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (nextAttemptAt == null) nextAttemptAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId(){return id;} public NotificationEventType getEventType(){return eventType;}
    public void setEventType(NotificationEventType v){eventType=v;} public String getChannel(){return channel;}
    public void setChannel(String v){channel=v;} public String getRecipientEmail(){return recipientEmail;}
    public void setRecipientEmail(String v){recipientEmail=v;} public String getSubject(){return subject;}
    public void setSubject(String v){subject=v;} public String getBody(){return body;}
    public void setBody(String v){body=v;} public String getStatus(){return status;}
    public void setStatus(String v){status=v;} public int getAttemptCount(){return attemptCount;}
    public void setAttemptCount(int v){attemptCount=v;} public String getProviderMessageId(){return providerMessageId;}
    public void setProviderMessageId(String v){providerMessageId=v;} public String getLastError(){return lastError;}
    public void setLastError(String v){lastError=v;} public LocalDateTime getNextAttemptAt(){return nextAttemptAt;}
    public void setNextAttemptAt(LocalDateTime v){nextAttemptAt=v;} public LocalDateTime getSentAt(){return sentAt;}
    public void setSentAt(LocalDateTime v){sentAt=v;} public Long getCustomerId(){return customerId;}
    public void setCustomerId(Long v){customerId=v;} public Long getOrderId(){return orderId;}
    public void setOrderId(Long v){orderId=v;} public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}
