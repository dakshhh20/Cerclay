CREATE TABLE payments (
                          id BIGINT NOT NULL AUTO_INCREMENT,

                          order_id BIGINT NOT NULL,

                          razorpay_order_id VARCHAR(100) NOT NULL,
                          razorpay_payment_id VARCHAR(100) NULL,

                          amount DECIMAL(10, 2) NOT NULL,
                          currency VARCHAR(10) NOT NULL DEFAULT 'INR',

                          payment_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
                          payment_method VARCHAR(50) NULL,

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          PRIMARY KEY (id),

                          CONSTRAINT uk_payments_razorpay_order_id
                              UNIQUE (razorpay_order_id),

                          CONSTRAINT uk_payments_razorpay_payment_id
                              UNIQUE (razorpay_payment_id),

                          CONSTRAINT fk_payments_order
                              FOREIGN KEY (order_id)
                                  REFERENCES orders(id)
                                  ON DELETE RESTRICT
                                  ON UPDATE CASCADE,

                          INDEX idx_payments_order (order_id),
                          INDEX idx_payments_status (payment_status),
                          INDEX idx_payments_created_at (created_at)
);