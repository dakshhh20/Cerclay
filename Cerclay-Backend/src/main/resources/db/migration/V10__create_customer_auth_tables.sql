-- ============================================================
-- V10: Customer Authentication Support
-- ============================================================

-- ------------------------------------------------------------
-- 1. Customer verification/account state
-- ------------------------------------------------------------

ALTER TABLE customers
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Existing customers who already have a password can remain
-- usable through the existing email/password authentication.
-- Verification will be required for the new flows where appropriate.


-- ------------------------------------------------------------
-- 2. OTP verification records
-- ------------------------------------------------------------

CREATE TABLE customer_otp_verifications (
                                            id BIGINT NOT NULL AUTO_INCREMENT,

                                            customer_id BIGINT NULL,

                                            destination VARCHAR(150) NOT NULL,
                                            destination_type VARCHAR(20) NOT NULL,

                                            purpose VARCHAR(40) NOT NULL,

                                            otp_hash VARCHAR(255) NOT NULL,

                                            expires_at DATETIME NOT NULL,
                                            verified_at DATETIME NULL,

                                            attempt_count INT NOT NULL DEFAULT 0,
                                            max_attempts INT NOT NULL DEFAULT 5,

                                            last_sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            PRIMARY KEY (id),

                                            CONSTRAINT fk_customer_otp_customer
                                                FOREIGN KEY (customer_id)
                                                    REFERENCES customers(id)
                                                    ON DELETE CASCADE
                                                    ON UPDATE CASCADE,

                                            CONSTRAINT chk_customer_otp_destination_type
                                                CHECK (
                                                    destination_type IN ('PHONE', 'EMAIL')
                                                    ),

                                            CONSTRAINT chk_customer_otp_purpose
                                                CHECK (
                                                    purpose IN (
                                                                'PHONE_LOGIN',
                                                                'PHONE_VERIFICATION',
                                                                'PASSWORD_RESET',
                                                                'EMAIL_VERIFICATION'
                                                        )
                                                    ),

                                            CONSTRAINT chk_customer_otp_attempt_count
                                                CHECK (attempt_count >= 0),

                                            CONSTRAINT chk_customer_otp_max_attempts
                                                CHECK (max_attempts > 0),

                                            INDEX idx_customer_otp_customer
                                                (customer_id),

                                            INDEX idx_customer_otp_destination
                                                (destination),

                                            INDEX idx_customer_otp_purpose
                                                (purpose),

                                            INDEX idx_customer_otp_expires_at
                                                (expires_at),

                                            INDEX idx_customer_otp_active_lookup
                                                (destination, purpose, verified_at, expires_at)
);


-- ------------------------------------------------------------
-- 3. Password reset tokens
-- ------------------------------------------------------------

CREATE TABLE customer_password_reset_tokens (
                                                id BIGINT NOT NULL AUTO_INCREMENT,

                                                customer_id BIGINT NOT NULL,

                                                token_hash VARCHAR(255) NOT NULL,

                                                expires_at DATETIME NOT NULL,
                                                used_at DATETIME NULL,

                                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                PRIMARY KEY (id),

                                                CONSTRAINT fk_password_reset_customer
                                                    FOREIGN KEY (customer_id)
                                                        REFERENCES customers(id)
                                                        ON DELETE CASCADE
                                                        ON UPDATE CASCADE,

                                                CONSTRAINT uk_password_reset_token_hash
                                                    UNIQUE (token_hash),

                                                INDEX idx_password_reset_customer
                                                    (customer_id),

                                                INDEX idx_password_reset_expires_at
                                                    (expires_at)
);