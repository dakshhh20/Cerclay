ALTER TABLE products
    ADD COLUMN color_group VARCHAR(150) NULL AFTER set_of_2_mrp,
    ADD COLUMN color_name VARCHAR(80) NULL AFTER color_group,
    ADD COLUMN color_hex VARCHAR(7) NULL AFTER color_name;

CREATE INDEX idx_products_color_group ON products(color_group);
