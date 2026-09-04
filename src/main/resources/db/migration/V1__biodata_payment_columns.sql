-- Collapse payment tracking onto bio_data (one flat table) and retire the
-- separate manual-verification table. Razorpay is now the only payment path.

ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS payment_status      VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS payment_amount      NUMERIC(10, 2);
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS payer_name          VARCHAR(255);
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS payer_email         VARCHAR(255);
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS payer_phone         VARCHAR(30);
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS razorpay_order_id   VARCHAR(64);
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(64);
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS paid_at             TIMESTAMP;

-- One Razorpay order per bio-data; retries overwrite the previous (abandoned) order.
ALTER TABLE bio_data ADD CONSTRAINT uk_bio_data_razorpay_order UNIQUE (razorpay_order_id);

-- Carry over anything already flagged paid under the old manual flow.
UPDATE bio_data SET payment_status = 'PAID' WHERE is_paid = TRUE;

DROP TABLE IF EXISTS payment_transactions;
