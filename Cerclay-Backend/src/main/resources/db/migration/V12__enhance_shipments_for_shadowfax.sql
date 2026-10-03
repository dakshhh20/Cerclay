ALTER TABLE shipments
    ADD COLUMN provider_code VARCHAR(50) NULL AFTER courier_name,
    ADD COLUMN external_shipment_id VARCHAR(100) NULL AFTER tracking_number,
    ADD COLUMN external_status VARCHAR(100) NULL AFTER shipment_status,
    ADD COLUMN external_status_display VARCHAR(150) NULL AFTER external_status,
    ADD COLUMN current_location VARCHAR(255) NULL AFTER external_status_display,
    ADD COLUMN latest_tracking_comment VARCHAR(500) NULL AFTER current_location,
    ADD COLUMN customer_track_url VARCHAR(500) NULL AFTER latest_tracking_comment,
    ADD COLUMN last_event_at DATETIME NULL AFTER customer_track_url,
    ADD COLUMN last_synced_at DATETIME NULL AFTER last_event_at;

CREATE INDEX idx_shipments_provider ON shipments(provider_code);
CREATE INDEX idx_shipments_external_id ON shipments(external_shipment_id);
CREATE INDEX idx_shipments_last_event ON shipments(last_event_at);

ALTER TABLE shipments
    ADD CONSTRAINT uk_shipments_provider_external_id
        UNIQUE (provider_code, external_shipment_id);