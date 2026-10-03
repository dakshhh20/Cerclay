CREATE TABLE orders (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        order_number VARCHAR(50) NOT NULL,

                        customer_id BIGINT NOT NULL,

                        subtotal DECIMAL(10, 2) NOT NULL,
                        discount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                        gst DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                        shipping_charge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                        total DECIMAL(10, 2) NOT NULL,

                        address_name VARCHAR(150) NOT NULL,
                        address_phone VARCHAR(20) NOT NULL,
                        address_line1 VARCHAR(255) NOT NULL,
                        address_line2 VARCHAR(255) NULL,
                        address_city VARCHAR(100) NOT NULL,
                        address_state VARCHAR(100) NOT NULL,
                        address_pincode VARCHAR(10) NOT NULL,
                        address_type VARCHAR(50) NULL,

                        payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        order_status VARCHAR(30) NOT NULL DEFAULT 'PLACED',

                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        PRIMARY KEY (id),

                        CONSTRAINT uk_orders_order_number
                            UNIQUE (order_number),

                        CONSTRAINT fk_orders_customer
                            FOREIGN KEY (customer_id)
                                REFERENCES customers(id)
                                ON DELETE RESTRICT
                                ON UPDATE CASCADE,

                        INDEX idx_orders_customer (customer_id),
                        INDEX idx_orders_payment_status (payment_status),
                        INDEX idx_orders_order_status (order_status),
                        INDEX idx_orders_created_at (created_at)
);


CREATE TABLE order_items (
                             id BIGINT NOT NULL AUTO_INCREMENT,

                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,

                             product_name VARCHAR(150) NOT NULL,
                             product_sku VARCHAR(100) NOT NULL,

                             quantity INT NOT NULL,

                             unit_price DECIMAL(10, 2) NOT NULL,
                             unit_mrp DECIMAL(10, 2) NOT NULL,
                             discount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

                             gst DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                             total DECIMAL(10, 2) NOT NULL,

                             PRIMARY KEY (id),

                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(id)
                                     ON DELETE CASCADE
                                     ON UPDATE CASCADE,

                             CONSTRAINT fk_order_items_product
                                 FOREIGN KEY (product_id)
                                     REFERENCES products(id)
                                     ON DELETE RESTRICT
                                     ON UPDATE CASCADE,

                             CONSTRAINT chk_order_items_quantity
                                 CHECK (quantity > 0),

                             INDEX idx_order_items_order (order_id),
                             INDEX idx_order_items_product (product_id)
);