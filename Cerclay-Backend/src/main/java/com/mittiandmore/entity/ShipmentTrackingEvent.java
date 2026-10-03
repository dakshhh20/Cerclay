package com.mittiandmore.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipment_tracking_events",
        indexes = {
                @Index(name = "idx_tracking_events_shipment_event_at", columnList = "shipment_id,event_at"),
                @Index(name = "idx_tracking_events_status", columnList = "shipment_status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shipment_tracking_event_key",
                columnNames = "event_key"
        )
)
public class ShipmentTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "event_key", nullable = false, length = 255, unique = true)
    private String eventKey;

    @Column(name = "external_status", length = 100)
    private String externalStatus;

    @Column(name = "external_status_display", length = 150)
    private String externalStatusDisplay;

    @Column(name = "shipment_status", nullable = false, length = 30)
    private String shipmentStatus;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "event_at")
    private LocalDateTime eventAt;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }
    public String getExternalStatusDisplay() { return externalStatusDisplay; }
    public void setExternalStatusDisplay(String externalStatusDisplay) { this.externalStatusDisplay = externalStatusDisplay; }
    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String shipmentStatus) { this.shipmentStatus = shipmentStatus; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
