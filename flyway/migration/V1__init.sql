CREATE TABLE IF NOT EXISTS app_user
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_in_catalog
(
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    barcode    VARCHAR(32)  NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    source     VARCHAR(32)  NOT NULL CHECK (source IN ('OPENFOODFACTS', 'USER_CONFIRMED', 'MANUAL')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventory_item
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    product_id      INT         NULL,
    custom_name     VARCHAR     NULL,
    expiration_date DATE        NOT NULL,
    consumed_at     TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ NULL,

    CONSTRAINT fk_users_items
        FOREIGN KEY (user_id)
            REFERENCES app_user (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_products_items
        FOREIGN KEY (product_id)
            REFERENCES product_in_catalog (id)
            ON DELETE RESTRICT,

    CHECK ((product_id IS NOT NULL AND custom_name IS NULL)
        OR (product_id IS NULL AND custom_name IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS product_candidate
(
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    barcode    VARCHAR(32)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(32)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'MERGED')),
    user_id    UUID         NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_users_product_candidates
        FOREIGN KEY (user_id)
            REFERENCES app_user (id)
            ON DELETE SET NULL

);

CREATE INDEX IF NOT EXISTS product_candidate_barcode_idx ON product_candidate (barcode);
CREATE INDEX IF NOT EXISTS product_candidate_user_id_idx ON product_candidate (user_id);
CREATE INDEX IF NOT EXISTS inventory_item_user_id_idx ON inventory_item (user_id);
CREATE INDEX IF NOT EXISTS inventory_item_product_id_idx ON inventory_item (product_id);
CREATE INDEX IF NOT EXISTS inventory_item_active_idx ON inventory_item (user_id) WHERE consumed_at IS NULL AND deleted_at IS NULL;
