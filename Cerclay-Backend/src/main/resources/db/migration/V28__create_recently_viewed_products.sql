-- ============================================================
-- V28: Customer Recently Viewed Products
-- ============================================================

CREATE TABLE recently_viewed_products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    last_viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT uk_recently_viewed_customer_product
        UNIQUE (customer_id, product_id),

    CONSTRAINT fk_recently_viewed_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_recently_viewed_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_recently_viewed_customer (customer_id),
    INDEX idx_recently_viewed_customer_last_viewed (customer_id, last_viewed_at),
    INDEX idx_recently_viewed_product (product_id)
);
