ALTER TABLE carts
    ADD COLUMN customer_id BIGINT NULL;

ALTER TABLE carts
    ADD CONSTRAINT fk_carts_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers(id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;

CREATE INDEX idx_carts_customer
    ON carts (customer_id);