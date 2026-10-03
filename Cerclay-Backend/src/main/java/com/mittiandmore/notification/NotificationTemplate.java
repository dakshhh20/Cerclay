package com.mittiandmore.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", uniqueConstraints = @UniqueConstraint(name="uk_notification_template_event", columnNames="event_type"))
public class NotificationTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name="event_type", nullable=false, length=50) private NotificationEventType eventType;
    @Column(nullable=false) private boolean enabled = true;
    @Column(name="subject_template", nullable=false, length=255) private String subjectTemplate;
    @Lob @Column(name="body_template", nullable=false, columnDefinition="LONGTEXT") private String bodyTemplate;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    @PrePersist @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public NotificationEventType getEventType(){return eventType;}
    public void setEventType(NotificationEventType v){eventType=v;} public boolean isEnabled(){return enabled;}
    public void setEnabled(boolean v){enabled=v;} public String getSubjectTemplate(){return subjectTemplate;}
    public void setSubjectTemplate(String v){subjectTemplate=v;} public String getBodyTemplate(){return bodyTemplate;}
    public void setBodyTemplate(String v){bodyTemplate=v;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
