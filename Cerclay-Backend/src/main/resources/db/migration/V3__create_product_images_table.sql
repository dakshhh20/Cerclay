CREATE TABLE product_images (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                product_id BIGINT NOT NULL,
                                image_url TEXT NOT NULL,
                                is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                display_order INT NOT NULL DEFAULT 0,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                PRIMARY KEY (id),

                                CONSTRAINT fk_product_images_product
                                    FOREIGN KEY (product_id)
                                        REFERENCES products(id)
                                        ON DELETE RESTRICT
                                        ON UPDATE CASCADE,

                                INDEX idx_product_images_product (product_id),
                                INDEX idx_product_images_order (product_id, display_order)
);