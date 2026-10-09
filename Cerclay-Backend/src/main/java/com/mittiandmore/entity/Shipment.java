package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "shipments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_shipments_order", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_shipments_tracking_number", columnNames = "tracking_number"),
        @UniqueConstraint(
            name = "uk_shipments_provider_external_id",
            columnNames = { "provider_code", "external_shipment_id" }
        ),
    },
    indexes = {
        @Index(name = "idx_shipments_status", columnList = "shipment_status"),
        @Index(name = "idx_shipments_tracking_number", columnList = "tracking_number"),
        @Index(name = "idx_shipments_provider", columnList = "provider_code"),
        @Index(name = "idx_shipments_external_id", columnList = "external_shipment_id"),
        @Index(name = "idx_shipments_created_at", columnList = "created_at"),
    }
)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /**
     * Human-readable courier name.
     * Example: Shadowfax
     */
    @Column(name = "courier_name", length = 100)
    private String courierName;

    /**
     * Stable courier/provider code.
     * Example: SHADOWFAX
     */
    @Column(name = "provider_code", length = 50)
    private String providerCode;

    /**
     * AWB / tracking number supplied by Shadowfax.
     */
    @Column(name = "tracking_number", length = 100, unique = true)
    private String trackingNumber;

    /**
     * External shipment ID returned by the courier.
     *
     * Shadowfax V4 tracking returns this as "id".
     */
    @Column(name = "external_shipment_id", length = 100)
    private String externalShipmentId;

    /**
     * Our own normalized shipment state.
     *
     * Example:
     * CREATED
     * PICKED_UP
     * IN_TRANSIT
     * OUT_FOR_DELIVERY
     * DELIVERED
     * RTO
     * CANCELLED
     */
    @Column(name = "shipment_status", nullable = false, length = 30)
    private String shipmentStatus = "CREATED";

    /**
     * Latest status ID received from Shadowfax.
     *
     * Example:
     * new
     * picked
     * ofd
     * delivered
     * rts
     */
    @Column(name = "external_status", length = 100)
    private String externalStatus;

    /**
     * Human-readable status returned by Shadowfax.
     *
     * Example:
     * "Out For Delivery"
     */
    @Column(name = "external_status_display", length = 150)
    private String externalStatusDisplay;

    /**
     * Latest location supplied by Shadowfax tracking.
     */
    @Column(name = "current_location", length = 255)
    private String currentLocation;

    /**
     * Latest tracking remarks/comments supplied by Shadowfax.
     */
    @Column(name = "latest_tracking_comment", length = 500)
    private String latestTrackingComment;

    /**
     * Customer-facing tracking URL supplied by Shadowfax.
     */
    @Column(name = "customer_track_url", length = 500)
    private String customerTrackUrl;

    /**
     * Timestamp of the latest shipment event received from Shadowfax.
     */
    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    /**
     * Timestamp when our backend last successfully synchronized
     * shipment information with Shadowfax.
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Shipment() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getExternalShipmentId() {
        return externalShipmentId;
    }

    public void setExternalShipmentId(String externalShipmentId) {
        this.externalShipmentId = externalShipmentId;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getExternalStatusDisplay() {
        return externalStatusDisplay;
    }

    public void setExternalStatusDisplay(String externalStatusDisplay) {
        this.externalStatusDisplay = externalStatusDisplay;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getLatestTrackingComment() {
        return latestTrackingComment;
    }

    public void setLatestTrackingComment(String latestTrackingComment) {
        this.latestTrackingComment = latestTrackingComment;
    }

    public String getCustomerTrackUrl() {
        return customerTrackUrl;
    }

    public void setCustomerTrackUrl(String customerTrackUrl) {
        this.customerTrackUrl = customerTrackUrl;
    }

    public LocalDateTime getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(LocalDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
