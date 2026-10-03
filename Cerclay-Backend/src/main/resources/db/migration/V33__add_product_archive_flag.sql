ALTER TABLE products
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_products_archived ON products (archived);
