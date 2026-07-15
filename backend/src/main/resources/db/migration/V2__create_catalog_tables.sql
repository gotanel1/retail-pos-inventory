CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_categories_normalized_name UNIQUE (normalized_name)
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES categories (id),
    sku VARCHAR(80) NOT NULL,
    barcode VARCHAR(64),
    name VARCHAR(180) NOT NULL,
    sale_price NUMERIC(19, 2) NOT NULL,
    low_stock_threshold INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT uk_products_barcode UNIQUE (barcode),
    CONSTRAINT ck_products_sale_price CHECK (sale_price >= 0),
    CONSTRAINT ck_products_low_stock_threshold CHECK (low_stock_threshold >= 0)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active_name ON products (active, name);

CREATE TABLE product_imports (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    raw_content TEXT NOT NULL,
    status VARCHAR(24) NOT NULL,
    total_rows INTEGER NOT NULL,
    valid_rows INTEGER NOT NULL,
    invalid_rows INTEGER NOT NULL,
    created_by UUID NOT NULL REFERENCES app_users (id),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_product_imports_status CHECK (status IN ('PREVIEWED', 'COMPLETED')),
    CONSTRAINT ck_product_imports_row_counts CHECK (
        total_rows >= 0 AND valid_rows >= 0 AND invalid_rows >= 0
        AND total_rows = valid_rows + invalid_rows
    )
);

CREATE INDEX idx_product_imports_created_by_status ON product_imports (created_by, status);
