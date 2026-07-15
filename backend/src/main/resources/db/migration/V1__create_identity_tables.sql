CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT ck_app_users_role CHECK (
        role IN ('OWNER', 'MANAGER', 'CASHIER', 'INVENTORY_STAFF')
    )
);

CREATE INDEX idx_app_users_active_role ON app_users (active, role);
