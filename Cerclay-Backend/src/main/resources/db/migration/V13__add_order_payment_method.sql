ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(30) NULL AFTER address_type;

UPDATE orders
SET payment_method = 'COD'
WHERE payment_method IS NULL;

ALTER TABLE orders
    MODIFY COLUMN payment_method VARCHAR(30) NOT NULL DEFAULT 'COD';

CREATE INDEX idx_orders_payment_method
    ON orders(payment_method);