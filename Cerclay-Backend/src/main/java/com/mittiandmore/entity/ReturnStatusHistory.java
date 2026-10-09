package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "return_status_history",
    indexes = @Index(name = "idx_return_history_request_created", columnList = "return_request_id, created_at")
)
public class ReturnStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, length = 50)
    private String actor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(ReturnRequest v) {
        returnRequest = v;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String v) {
        fromStatus = v;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String v) {
        toStatus = v;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String v) {
        note = v;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String v) {
        actor = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
