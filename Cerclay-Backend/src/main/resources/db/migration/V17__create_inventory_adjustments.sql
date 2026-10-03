CREATE TABLE inventory_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    change_quantity INT NOT NULL,
    change_type VARCHAR(40) NOT NULL,
    reason VARCHAR(255) NULL,
    actor VARCHAR(180) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_inventory_adjustments_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_inventory_adjustments_product (product_id),
    INDEX idx_inventory_adjustments_created (created_at)
);
