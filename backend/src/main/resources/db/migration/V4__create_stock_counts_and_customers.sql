CREATE TABLE inventory_counts (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    counted_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL REFERENCES app_users (id),
    created_at TIMESTAMPTZ NOT NULL,
    approved_by UUID REFERENCES app_users (id),
    approved_at TIMESTAMPTZ,
    CONSTRAINT ck_inventory_counts_status CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_inventory_counts_approval CHECK (
        (status = 'SUBMITTED' AND approved_by IS NULL AND approved_at IS NULL)
        OR (status IN ('APPROVED', 'REJECTED') AND approved_by IS NOT NULL AND approved_at IS NOT NULL)
    )
);

CREATE TABLE inventory_count_items (
    id UUID PRIMARY KEY,
    count_id UUID NOT NULL REFERENCES inventory_counts (id),
    product_id UUID NOT NULL REFERENCES products (id),
    expected_on_hand INTEGER NOT NULL,
    counted_quantity INTEGER NOT NULL,
    difference INTEGER NOT NULL,
    CONSTRAINT uk_inventory_count_items_product UNIQUE (count_id, product_id),
    CONSTRAINT ck_inventory_count_items_expected CHECK (expected_on_hand >= 0),
    CONSTRAINT ck_inventory_count_items_counted CHECK (counted_quantity >= 0),
    CONSTRAINT ck_inventory_count_items_difference CHECK (difference = counted_quantity - expected_on_hand)
);

CREATE INDEX idx_inventory_counts_status_created ON inventory_counts (status, created_at DESC);
CREATE INDEX idx_inventory_count_items_product ON inventory_count_items (product_id);

CREATE TRIGGER trg_inventory_count_items_immutable
BEFORE UPDATE OR DELETE ON inventory_count_items
FOR EACH ROW EXECUTE FUNCTION prevent_immutable_inventory_mutation();

CREATE FUNCTION protect_inventory_count_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'inventory_counts is immutable';
    END IF;
    IF OLD.status <> 'SUBMITTED' THEN
        RAISE EXCEPTION 'completed inventory count is immutable';
    END IF;
    IF NEW.id <> OLD.id OR NEW.reason <> OLD.reason OR NEW.counted_at <> OLD.counted_at
       OR NEW.created_by <> OLD.created_by OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'inventory count evidence cannot be changed';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inventory_counts_history
BEFORE UPDATE OR DELETE ON inventory_counts
FOR EACH ROW EXECUTE FUNCTION protect_inventory_count_history();

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    phone VARCHAR(32),
    normalized_phone VARCHAR(32),
    note VARCHAR(500),
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    consent_updated_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    anonymized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_customers_normalized_phone UNIQUE (normalized_phone),
    CONSTRAINT ck_customers_anonymized CHECK (
        (active = TRUE AND anonymized_at IS NULL)
        OR (active = FALSE AND anonymized_at IS NOT NULL AND phone IS NULL AND normalized_phone IS NULL
            AND note IS NULL AND marketing_consent = FALSE)
    )
);

CREATE INDEX idx_customers_active_name ON customers (active, name);
