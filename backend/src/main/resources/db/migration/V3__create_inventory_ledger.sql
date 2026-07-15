CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    normalized_name VARCHAR(180) NOT NULL,
    phone VARCHAR(32),
    note VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_suppliers_normalized_name UNIQUE (normalized_name)
);

CREATE TABLE inventory_balances (
    product_id UUID PRIMARY KEY REFERENCES products (id),
    on_hand INTEGER NOT NULL DEFAULT 0,
    reserved INTEGER NOT NULL DEFAULT 0,
    average_cost NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_inventory_balances_on_hand CHECK (on_hand >= 0),
    CONSTRAINT ck_inventory_balances_reserved CHECK (reserved >= 0 AND reserved <= on_hand),
    CONSTRAINT ck_inventory_balances_average_cost CHECK (average_cost >= 0)
);

INSERT INTO inventory_balances (product_id, updated_at)
SELECT id, CURRENT_TIMESTAMP FROM products;

CREATE TABLE goods_receipts (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES suppliers (id),
    reference_number VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(500),
    received_by UUID NOT NULL REFERENCES app_users (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_goods_receipts_reference UNIQUE (reference_number),
    CONSTRAINT ck_goods_receipts_status CHECK (status IN ('POSTED'))
);

CREATE TABLE goods_receipt_items (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES goods_receipts (id),
    product_id UUID NOT NULL REFERENCES products (id),
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(19, 4) NOT NULL,
    CONSTRAINT uk_goods_receipt_items_product UNIQUE (receipt_id, product_id),
    CONSTRAINT ck_goods_receipt_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_goods_receipt_items_unit_cost CHECK (unit_cost >= 0)
);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products (id),
    movement_type VARCHAR(32) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    on_hand_after INTEGER NOT NULL,
    reserved_after INTEGER NOT NULL,
    unit_cost NUMERIC(19, 4),
    average_cost_after NUMERIC(19, 4) NOT NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id UUID NOT NULL,
    reason VARCHAR(500),
    actor_user_id UUID NOT NULL REFERENCES app_users (id),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_stock_movements_type CHECK (
        movement_type IN ('OPENING', 'RECEIVE', 'SALE', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT')
    ),
    CONSTRAINT ck_stock_movements_quantity CHECK (quantity_delta <> 0),
    CONSTRAINT ck_stock_movements_on_hand CHECK (on_hand_after >= 0),
    CONSTRAINT ck_stock_movements_reserved CHECK (reserved_after >= 0 AND reserved_after <= on_hand_after),
    CONSTRAINT ck_stock_movements_cost CHECK (
        (unit_cost IS NULL OR unit_cost >= 0) AND average_cost_after >= 0
    )
);

CREATE INDEX idx_inventory_balances_low_stock ON inventory_balances (on_hand);
CREATE INDEX idx_goods_receipts_received_at ON goods_receipts (received_at DESC);
CREATE INDEX idx_stock_movements_product_occurred ON stock_movements (product_id, occurred_at DESC);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference_type, reference_id);

CREATE FUNCTION prevent_immutable_inventory_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% is immutable; append a compensating business transaction instead', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_movements_immutable
BEFORE UPDATE OR DELETE ON stock_movements
FOR EACH ROW EXECUTE FUNCTION prevent_immutable_inventory_mutation();

CREATE TRIGGER trg_goods_receipts_immutable
BEFORE UPDATE OR DELETE ON goods_receipts
FOR EACH ROW EXECUTE FUNCTION prevent_immutable_inventory_mutation();

CREATE TRIGGER trg_goods_receipt_items_immutable
BEFORE UPDATE OR DELETE ON goods_receipt_items
FOR EACH ROW EXECUTE FUNCTION prevent_immutable_inventory_mutation();
