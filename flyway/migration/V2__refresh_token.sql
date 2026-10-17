CREATE TABLE IF NOT EXISTS refresh_token
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_users_refresh_tokens
    FOREIGN KEY (user_id)
    REFERENCES app_user (id)
    ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS refresh_token_user_id_idx ON refresh_token (user_id);
CREATE INDEX IF NOT EXISTS refresh_token_expires_at_idx ON refresh_token (expires_at);