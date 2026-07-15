ALTER TABLE app_users ADD COLUMN manager_pin_hash VARCHAR(100);

CREATE TABLE store_settings (
    id SMALLINT PRIMARY KEY,
    store_name VARCHAR(180) NOT NULL,
    vat_enabled BOOLEAN NOT NULL,
    vat_rate NUMERIC(5, 2) NOT NULL,
    receipt_footer VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_store_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_store_settings_vat_rate CHECK (vat_rate >= 0 AND vat_rate <= 100)
);

INSERT INTO store_settings (id, store_name, vat_enabled, vat_rate, updated_at)
VALUES (1, 'Retail POS Demo', TRUE, 7.00, CURRENT_TIMESTAMP);

CREATE SEQUENCE receipt_number_seq START WITH 1;

CREATE TABLE sales (
    id UUID PRIMARY KEY,
    receipt_number VARCHAR(40),
    status VARCHAR(24) NOT NULL,
    customer_id UUID REFERENCES customers (id),
    subtotal NUMERIC(19, 2) NOT NULL,
    discount_type VARCHAR(16),
    discount_value NUMERIC(19, 2),
    discount_amount NUMERIC(19, 2) NOT NULL,
    vat_enabled BOOLEAN NOT NULL,
    vat_rate NUMERIC(5, 2) NOT NULL,
    vat_amount NUMERIC(19, 2) NOT NULL,
    total NUMERIC(19, 2) NOT NULL,
    cash_received NUMERIC(19, 2),
    change_amount NUMERIC(19, 2),
    checkout_idempotency_key VARCHAR(100),
    discount_approved_by UUID REFERENCES app_users (id),
    created_by UUID NOT NULL REFERENCES app_users (id),
    completed_by UUID REFERENCES app_users (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_receipt_number UNIQUE (receipt_number),
    CONSTRAINT uk_sales_idempotency_key UNIQUE (checkout_idempotency_key),
    CONSTRAINT ck_sales_status CHECK (status IN ('DRAFT', 'AWAITING_PAYMENT', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_sales_discount_type CHECK (discount_type IS NULL OR discount_type IN ('AMOUNT', 'PERCENT')),
    CONSTRAINT ck_sales_amounts CHECK (subtotal >= 0 AND discount_amount >= 0 AND vat_amount >= 0 AND total >= 0),
    CONSTRAINT ck_sales_completed CHECK (
        (status <> 'COMPLETED' AND completed_by IS NULL AND completed_at IS NULL AND receipt_number IS NULL)
        OR (status = 'COMPLETED' AND completed_by IS NOT NULL AND completed_at IS NOT NULL AND receipt_number IS NOT NULL
            AND cash_received IS NOT NULL AND change_amount IS NOT NULL AND checkout_idempotency_key IS NOT NULL)
    )
);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales (id),
    product_id UUID NOT NULL REFERENCES products (id),
    sku_snapshot VARCHAR(80) NOT NULL,
    name_snapshot VARCHAR(180) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    unit_cost_snapshot NUMERIC(19, 4),
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_sale_items_product UNIQUE (sale_id, product_id),
    CONSTRAINT ck_sale_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_sale_items_amounts CHECK (unit_price >= 0 AND line_total = unit_price * quantity),
    CONSTRAINT ck_sale_items_cost CHECK (unit_cost_snapshot IS NULL OR unit_cost_snapshot >= 0)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales (id),
    payment_method VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    provider_event_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_payments_method CHECK (payment_method IN ('CASH', 'PROMPTPAY')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0),
    CONSTRAINT uk_payments_provider_event UNIQUE (provider_event_id)
);

CREATE INDEX idx_sales_status_created ON sales (status, created_at DESC);
CREATE INDEX idx_sales_customer_completed ON sales (customer_id, completed_at DESC);
CREATE INDEX idx_sale_items_product ON sale_items (product_id);
CREATE INDEX idx_payments_sale ON payments (sale_id);

CREATE FUNCTION protect_completed_sale()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' OR OLD.status = 'COMPLETED' THEN
        RAISE EXCEPTION 'completed sale history is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sales_completed_immutable
BEFORE UPDATE OR DELETE ON sales
FOR EACH ROW EXECUTE FUNCTION protect_completed_sale();

CREATE FUNCTION protect_completed_sale_item()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' OR EXISTS (SELECT 1 FROM sales WHERE id = OLD.sale_id AND status = 'COMPLETED') THEN
        RAISE EXCEPTION 'completed sale items are immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sale_items_completed_immutable
BEFORE UPDATE OR DELETE ON sale_items
FOR EACH ROW EXECUTE FUNCTION protect_completed_sale_item();
