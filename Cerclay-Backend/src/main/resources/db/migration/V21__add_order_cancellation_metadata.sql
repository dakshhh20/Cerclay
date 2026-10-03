ALTER TABLE orders
    ADD COLUMN cancelled_at DATETIME NULL,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL,
    ADD COLUMN cancelled_by VARCHAR(50) NULL;

CREATE INDEX idx_orders_cancelled_at ON orders(cancelled_at);
