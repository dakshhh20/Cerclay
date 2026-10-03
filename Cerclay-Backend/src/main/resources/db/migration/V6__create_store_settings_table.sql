CREATE TABLE store_settings (
                                id BIGINT NOT NULL AUTO_INCREMENT,

                                gst_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
                                shipping_charge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                free_shipping_threshold DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                minimum_order_value DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                PRIMARY KEY (id)
);

INSERT INTO store_settings (
    gst_rate,
    shipping_charge,
    free_shipping_threshold,
    minimum_order_value
)
VALUES (
           0.00,
           0.00,
           0.00,
           0.00
       );