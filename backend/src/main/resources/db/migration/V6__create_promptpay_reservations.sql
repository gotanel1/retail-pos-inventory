ALTER TABLE sales DROP CONSTRAINT ck_sales_completed;
ALTER TABLE sales ADD CONSTRAINT ck_sales_completed CHECK (
    (status <> 'COMPLETED' AND completed_by IS NULL AND completed_at IS NULL AND receipt_number IS NULL)
    OR (status = 'COMPLETED' AND completed_by IS NOT NULL AND completed_at IS NOT NULL AND receipt_number IS NOT NULL)
);
ALTER TABLE sales ADD CONSTRAINT ck_sales_cash_pair CHECK (
    (cash_received IS NULL AND change_amount IS NULL) OR (cash_received IS NOT NULL AND change_amount IS NOT NULL)
);

ALTER TABLE payments ADD COLUMN provider_payment_intent_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN qr_code_data_url TEXT;
ALTER TABLE payments ADD COLUMN expires_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE payments SET updated_at = COALESCE(completed_at, created_at);
ALTER TABLE payments ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE payments ADD CONSTRAINT uk_payments_provider_intent UNIQUE (provider_payment_intent_id);

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales (id),
    status VARCHAR(24) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL REFERENCES app_users (id),
    created_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_reservations_sale UNIQUE (sale_id),
    CONSTRAINT ck_reservations_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_reservations_closed CHECK (
        (status = 'ACTIVE' AND closed_at IS NULL) OR (status <> 'ACTIVE' AND closed_at IS NOT NULL)
    )
);

CREATE TABLE stock_reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES stock_reservations (id),
    product_id UUID NOT NULL REFERENCES products (id),
    quantity INTEGER NOT NULL,
    CONSTRAINT uk_reservation_items_product UNIQUE (reservation_id, product_id),
    CONSTRAINT ck_reservation_items_quantity CHECK (quantity > 0)
);

CREATE TABLE stripe_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    payment_intent_id VARCHAR(255),
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_reservations_expiry ON stock_reservations (status, expires_at);
CREATE INDEX idx_reservation_items_product ON stock_reservation_items (product_id);
