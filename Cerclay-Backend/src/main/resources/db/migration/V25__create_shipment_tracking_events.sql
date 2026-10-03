CREATE TABLE shipment_tracking_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    external_status VARCHAR(100),
    external_status_display VARCHAR(150),
    shipment_status VARCHAR(30) NOT NULL,
    location VARCHAR(255),
    remarks VARCHAR(500),
    event_at DATETIME,
    source VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shipment_tracking_event_key UNIQUE (event_key),
    CONSTRAINT fk_shipment_tracking_events_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    INDEX idx_tracking_events_shipment_event_at (shipment_id, event_at),
    INDEX idx_tracking_events_status (shipment_status)
);
