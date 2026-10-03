-- V1__initial_schema.sql
-- Baseline schema matching current entities:
-- Admin, Customer, Product, Address, Cart, CartItem
-- Generated to match Hibernate's current auto-created schema exactly,
-- so switching from ddl-auto=update to ddl-auto=validate does not fail.

-- =========================================================
-- admins
-- =========================================================
CREATE TABLE admins (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        role VARCHAR(255) NOT NULL DEFAULT 'ADMIN',
                        CONSTRAINT uq_admins_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- customers
-- =========================================================
CREATE TABLE customers (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           email VARCHAR(255),
                           phone VARCHAR(255),
                           password VARCHAR(255),
                           google_id VARCHAR(255),
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           role VARCHAR(255) NOT NULL DEFAULT 'CUSTOMER',
                           CONSTRAINT uq_customers_email UNIQUE (email),
                           CONSTRAINT uq_customers_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- products
-- =========================================================
CREATE TABLE products (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          price DECIMAL(10,2) NOT NULL,
                          mrp DECIMAL(10,2),
                          stock INT NOT NULL DEFAULT 0,
                          category VARCHAR(255) NOT NULL,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          image TEXT,
                          rating DECIMAL(2,1),
                          reviews INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Index for storefront filtering by category (item 5 / item 15 in spec)
CREATE INDEX idx_products_category ON products (category);

-- Index for filtering active products, since every storefront query
-- excludes inactive products
CREATE INDEX idx_products_active ON products (active);

-- =========================================================
-- addresses
-- =========================================================
CREATE TABLE addresses (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           phone VARCHAR(255) NOT NULL,
                           house VARCHAR(255) NOT NULL,
                           street VARCHAR(255) NOT NULL,
                           city VARCHAR(255) NOT NULL,
                           state VARCHAR(255) NOT NULL,
                           pincode VARCHAR(255) NOT NULL,
                           address_type VARCHAR(255) NOT NULL,
                           default_address BOOLEAN NOT NULL DEFAULT FALSE,
                           customer_id BIGINT NOT NULL,
                           CONSTRAINT fk_addresses_customer
                               FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Speeds up "get all addresses for this customer" (AddressController)
CREATE INDEX idx_addresses_customer_id ON addresses (customer_id);

-- =========================================================
-- carts
-- =========================================================
CREATE TABLE carts (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       guest_id VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Speeds up findByGuestId(), used on every cart request
CREATE INDEX idx_carts_guest_id ON carts (guest_id);

-- =========================================================
-- cart_items
-- =========================================================
CREATE TABLE cart_items (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            quantity INT NOT NULL,
                            cart_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            CONSTRAINT fk_cart_items_cart
                                FOREIGN KEY (cart_id) REFERENCES carts (id)
                                    ON DELETE CASCADE,
                            CONSTRAINT fk_cart_items_product
                                FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);