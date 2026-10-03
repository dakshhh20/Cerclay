CREATE TABLE return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    customer_note VARCHAR(1000) NULL,
    admin_note VARCHAR(1000) NULL,
    requested_at DATETIME NOT NULL,
    approved_at DATETIME NULL,
    rejected_at DATETIME NULL,
    received_at DATETIME NULL,
    closed_at DATETIME NULL,
    refund_requested_amount DECIMAL(10,2) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_return_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_return_requests_order ON return_requests(order_id);
CREATE INDEX idx_return_requests_customer ON return_requests(customer_id);
CREATE INDEX idx_return_requests_status ON return_requests(status);

CREATE TABLE return_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_request_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_item_request FOREIGN KEY (return_request_id) REFERENCES return_requests(id),
    CONSTRAINT fk_return_item_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT uk_return_item_request_order_item UNIQUE (return_request_id, order_item_id)
);

CREATE INDEX idx_return_items_request ON return_items(return_request_id);
CREATE INDEX idx_return_items_order_item ON return_items(order_item_id);

CREATE TABLE return_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_request_id BIGINT NOT NULL,
    from_status VARCHAR(40) NULL,
    to_status VARCHAR(40) NOT NULL,
    note VARCHAR(1000) NULL,
    actor VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_history_request FOREIGN KEY (return_request_id) REFERENCES return_requests(id)
);

CREATE INDEX idx_return_history_request_created ON return_status_history(return_request_id, created_at);

CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_request_id BIGINT NULL,
    order_id BIGINT NOT NULL,
    payment_id BIGINT NULL,
    razorpay_refund_id VARCHAR(100) NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    receipt VARCHAR(100) NOT NULL,
    failure_reason VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refund_return FOREIGN KEY (return_request_id) REFERENCES return_requests(id),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT uk_refund_razorpay_id UNIQUE (razorpay_refund_id),
    CONSTRAINT uk_refund_receipt UNIQUE (receipt)
);

CREATE INDEX idx_refunds_order ON refunds(order_id);
CREATE INDEX idx_refunds_return ON refunds(return_request_id);
CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_refunds_status ON refunds(status);
