ALTER TABLE payments MODIFY COLUMN razorpay_order_id VARCHAR(100) NULL;
ALTER TABLE payments ADD COLUMN cashfree_order_id VARCHAR(100) NULL UNIQUE;
ALTER TABLE payments ADD COLUMN cashfree_payment_session_id VARCHAR(500) NULL;
ALTER TABLE payments ADD COLUMN cashfree_payment_id VARCHAR(100) NULL UNIQUE;
ALTER TABLE refunds ADD COLUMN cashfree_refund_id VARCHAR(100) NULL UNIQUE;
