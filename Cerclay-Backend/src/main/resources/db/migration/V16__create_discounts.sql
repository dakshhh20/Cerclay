CREATE TABLE discounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    type VARCHAR(20) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    minimum_order_value DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    max_discount DECIMAL(10,2) NULL,
    starts_at DATETIME NULL,
    expires_at DATETIME NULL,
    usage_limit INT NULL,
    usage_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_discounts_code UNIQUE (code),
    INDEX idx_discounts_active (active)
);
