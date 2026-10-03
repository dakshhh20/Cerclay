package com.mittiandmore.dto;

import java.time.LocalDateTime;

public class ShipmentTrackingEventResponse {
    private String shipmentStatus;
    private String externalStatus;
    private String externalStatusDisplay;
    private String location;
    private String remarks;
    private LocalDateTime eventAt;
    private String source;

    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String value) { this.shipmentStatus = value; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String value) { this.externalStatus = value; }
    public String getExternalStatusDisplay() { return externalStatusDisplay; }
    public void setExternalStatusDisplay(String value) { this.externalStatusDisplay = value; }
    public String getLocation() { return location; }
    public void setLocation(String value) { this.location = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { this.remarks = value; }
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime value) { this.eventAt = value; }
    public String getSource() { return source; }
    public void setSource(String value) { this.source = value; }
}
