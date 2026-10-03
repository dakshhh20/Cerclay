CREATE TABLE payment_webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_webhook_event_id UNIQUE (event_id),
    INDEX idx_payment_webhook_received (received_at)
);
