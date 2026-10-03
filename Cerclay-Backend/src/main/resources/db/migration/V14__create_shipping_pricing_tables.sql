CREATE TABLE shipping_zones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    shadowfax_base_rate DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    customer_charge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    free_delivery_threshold DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    cod_charge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_shipping_zones_code UNIQUE (code),
    INDEX idx_shipping_zones_active (active)
);

CREATE TABLE shipping_pincodes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pincode VARCHAR(6) NOT NULL,
    zone_id BIGINT NOT NULL,
    customer_charge_override DECIMAL(10, 2) NULL,
    free_delivery_threshold_override DECIMAL(10, 2) NULL,
    cod_charge_override DECIMAL(10, 2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_shipping_pincodes_pincode UNIQUE (pincode),
    CONSTRAINT fk_shipping_pincodes_zone
        FOREIGN KEY (zone_id) REFERENCES shipping_zones(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_shipping_pincodes_zone (zone_id),
    INDEX idx_shipping_pincodes_active (active)
);

INSERT INTO shipping_zones
    (code, name, shadowfax_base_rate, customer_charge, free_delivery_threshold, cod_charge, active)
VALUES
    ('A', 'Local', 39.00, 39.00, 0.00, 0.00, TRUE),
    ('B', 'Regional', 49.00, 49.00, 0.00, 0.00, TRUE),
    ('C', 'Metro-to-Metro', 59.00, 59.00, 0.00, 0.00, TRUE),
    ('D', 'Rest of India', 59.00, 59.00, 0.00, 0.00, TRUE),
    ('E', 'Special', 69.00, 69.00, 0.00, 0.00, TRUE);

-- The warehouse pickup pincode is known to be intracity for the configured pickup address.
-- Other pincodes are intentionally not guessed; the admin must map them to the correct
-- Shadowfax zone using the account's current rate-card information.
INSERT INTO shipping_pincodes (pincode, zone_id, active)
SELECT '208002', id, TRUE
FROM shipping_zones
WHERE code = 'A';
