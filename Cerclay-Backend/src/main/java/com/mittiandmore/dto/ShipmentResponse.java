package com.mittiandmore.dto;

import java.time.LocalDateTime;

public class ShipmentResponse {

    private Long id;
    private Long orderId;
    private String courierName;
    private String providerCode;
    private String trackingNumber;
    private String shipmentStatus;
    private String externalStatus;
    private String externalStatusDisplay;
    private String currentLocation;
    private String latestTrackingComment;
    private String customerTrackUrl;
    private LocalDateTime lastEventAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ShipmentResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String shipmentStatus) { this.shipmentStatus = shipmentStatus; }

    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }

    public String getExternalStatusDisplay() { return externalStatusDisplay; }
    public void setExternalStatusDisplay(String externalStatusDisplay) { this.externalStatusDisplay = externalStatusDisplay; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public String getLatestTrackingComment() { return latestTrackingComment; }
    public void setLatestTrackingComment(String latestTrackingComment) { this.latestTrackingComment = latestTrackingComment; }

    public String getCustomerTrackUrl() { return customerTrackUrl; }
    public void setCustomerTrackUrl(String customerTrackUrl) { this.customerTrackUrl = customerTrackUrl; }

    public LocalDateTime getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(LocalDateTime lastEventAt) { this.lastEventAt = lastEventAt; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
