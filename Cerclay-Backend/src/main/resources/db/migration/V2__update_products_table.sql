ALTER TABLE products
    ADD COLUMN sku VARCHAR(100) NULL,
    ADD COLUMN slug VARCHAR(180) NULL,
    ADD COLUMN created_at DATETIME NULL,
    ADD COLUMN updated_at DATETIME NULL;

UPDATE products
SET
    sku = CONCAT('SKU-', id),
    slug = CONCAT('product-', id),
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE
    sku IS NULL
   OR slug IS NULL
   OR created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE products
    MODIFY COLUMN sku VARCHAR(100) NOT NULL,
    MODIFY COLUMN slug VARCHAR(180) NOT NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;

CREATE UNIQUE INDEX idx_products_sku
    ON products (sku);

CREATE UNIQUE INDEX idx_products_slug
    ON products (slug);