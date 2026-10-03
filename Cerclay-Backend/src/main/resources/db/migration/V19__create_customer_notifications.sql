CREATE TABLE notification_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    subject_template VARCHAR(255) NOT NULL,
    body_template LONGTEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_template_event UNIQUE (event_type)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(255) NULL,
    last_error VARCHAR(1000) NULL,
    next_attempt_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    customer_id BIGINT NULL,
    order_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_notifications_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE INDEX idx_notifications_status_next_attempt ON notifications(status, next_attempt_at);
CREATE INDEX idx_notifications_customer ON notifications(customer_id);
CREATE INDEX idx_notifications_order ON notifications(order_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

INSERT INTO notification_templates(event_type, enabled, subject_template, body_template, updated_at) VALUES
('ORDER_PLACED', TRUE, 'Cerclay order {{orderNumber}} confirmed', 'Hi {{customerName}},\n\nYour Cerclay order {{orderNumber}} has been placed successfully.\nTotal: ₹{{total}}\nPayment method: {{paymentMethod}}\n\nWe will keep you updated as your order progresses.\n\nCerclay', NOW()),
('ORDER_SHIPPED', TRUE, 'Your Cerclay order {{orderNumber}} has shipped', 'Hi {{customerName}},\n\nYour order {{orderNumber}} has shipped.\nTracking number: {{trackingNumber}}\n{{trackingUrl}}\n\nCerclay', NOW()),
('ORDER_OUT_FOR_DELIVERY', TRUE, 'Your Cerclay order {{orderNumber}} is out for delivery', 'Hi {{customerName}},\n\nYour order {{orderNumber}} is out for delivery today.\nTracking number: {{trackingNumber}}\n{{trackingUrl}}\n\nCerclay', NOW()),
('ORDER_DELIVERED', TRUE, 'Your Cerclay order {{orderNumber}} was delivered', 'Hi {{customerName}},\n\nYour order {{orderNumber}} has been delivered.\n\nThank you for shopping with Cerclay.', NOW()),
('ORDER_CANCELLED', TRUE, 'Your Cerclay order {{orderNumber}} was cancelled', 'Hi {{customerName}},\n\nYour order {{orderNumber}} has been cancelled.\nIf a payment was already collected, the applicable refund will be handled according to the order refund process.\n\nCerclay', NOW());
