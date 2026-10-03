CREATE TABLE product_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    rating INT NOT NULL,
    review VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_purchase BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_review_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_product_reviews_product_status (product_id, status),
    INDEX idx_product_reviews_customer (customer_id)
);
