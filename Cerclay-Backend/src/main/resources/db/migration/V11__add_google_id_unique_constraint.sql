-- ============================================================
-- V11: Google Login identity constraint
-- ============================================================

ALTER TABLE customers
    ADD CONSTRAINT uq_customers_google_id UNIQUE (google_id);
