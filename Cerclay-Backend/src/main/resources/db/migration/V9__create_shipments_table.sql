CREATE TABLE shipments (
                           id BIGINT NOT NULL AUTO_INCREMENT,

                           order_id BIGINT NOT NULL,

                           courier_name VARCHAR(100) NULL,
                           tracking_number VARCHAR(100) NULL,

                           shipment_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',

                           shipped_at DATETIME NULL,
                           delivered_at DATETIME NULL,

                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           PRIMARY KEY (id),

                           CONSTRAINT uk_shipments_order
                               UNIQUE (order_id),

                           CONSTRAINT uk_shipments_tracking_number
                               UNIQUE (tracking_number),

                           CONSTRAINT fk_shipments_order
                               FOREIGN KEY (order_id)
                                   REFERENCES orders(id)
                                   ON DELETE RESTRICT
                                   ON UPDATE CASCADE,

                           INDEX idx_shipments_status (shipment_status),
                           INDEX idx_shipments_tracking_number (tracking_number),
                           INDEX idx_shipments_created_at (created_at)
);