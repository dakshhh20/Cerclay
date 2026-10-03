ALTER TABLE orders
    ADD COLUMN coupon_code VARCHAR(40) NULL AFTER discount;

CREATE INDEX idx_orders_coupon_code ON orders(coupon_code);
