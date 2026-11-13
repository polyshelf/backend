CREATE TABLE fetch_history
(
    source     VARCHAR(32) PRIMARY KEY,
    revision   VARCHAR(64) NOT NULL,
    next_row   BIGINT      NOT NULL DEFAULT 0 CHECK (next_row >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
